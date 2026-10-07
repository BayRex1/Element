package elemsocial.com.feature.main.presentation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher

private data class NativeChat(
    val id: Int,
    val type: Int,
    val name: String,
    val avatar: String? = null,
    val lastMessage: String = "",
    val notifications: Int = 0
)

private data class NativeMessage(
    val mid: Long? = null,
    val tempMid: Long? = null,
    val uid: Int? = null,
    val author: String = "",
    val text: String = "",
    val date: String = "",
    val edited: Boolean = false,
    val reactions: Map<String, List<String>> = emptyMap()
)

@Composable
fun MessengerScreen(
    socketClient: ElementSocketClient,
    accountId: Int?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("messenger_native", Context.MODE_PRIVATE)
    }
    var keyword by remember { mutableStateOf(prefs.getString("keyword", "").orEmpty()) }
    var keyInput by remember { mutableStateOf("") }
    var chats by remember { mutableStateOf<List<NativeChat>>(emptyList()) }
    var selected by remember { mutableStateOf<NativeChat?>(null) }
    var messages by remember { mutableStateOf<List<NativeMessage>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var loadingChat by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<NativeMessage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showKeyDialog by remember { mutableStateOf(keyword.isBlank()) }
    val listState = rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun map(value: Any?): Map<String, Any?>? = value as? Map<String, Any?>
    fun int(value: Any?): Int? = when (value) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }
    fun long(value: Any?): Long? = when (value) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
    fun string(value: Any?): String = value?.toString().orEmpty()
    fun bytes(value: Any?): ByteArray? = when (value) {
        is ByteArray -> value
        is UByteArray -> value.toByteArray()
        is List<*> -> value.mapNotNull { int(it)?.toByte() }.toByteArray()
        else -> null
    }

    suspend fun decrypt(value: Any?): String? = withContext(Dispatchers.Default) {
        if (value == null) return@withContext null
        val plain = string(value)
        if (plain.startsWith("{") || plain.startsWith("[")) return@withContext plain
        val encrypted = bytes(value) ?: return@withContext plain
        if (keyword.length != 32 || encrypted.size <= 16) return@withContext null
        runCatching {
            val key = SecretKeySpec(keyword.toByteArray(StandardCharsets.UTF_8), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(encrypted.copyOfRange(0, 16)))
            String(cipher.doFinal(encrypted.copyOfRange(16, encrypted.size)), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    fun parseMessage(raw: Map<String, Any?>): NativeMessage? {
        val decrypted = raw["decrypted"] as? Map<*, *>
        val text = string(decrypted?.get("text")).ifBlank { string(raw["message"]) }
        return NativeMessage(
            mid = long(raw["mid"]),
            tempMid = long(raw["temp_mid"]),
            uid = int(raw["uid"]),
            author = string((raw["author"] as? Map<*, *>)?.get("name")),
            text = text,
            date = string(raw["date"]),
            edited = decrypted?.get("is_edited") == true,
            reactions = emptyMap()
        )
    }

    suspend fun loadChats() {
        runCatching {
            val response = socketClient.sendRequest(mapOf("type" to "messenger", "action" to "load_chats"))
            val rawChats = response["chats"] as? List<*> ?: emptyList<Any?>()
            chats = rawChats.mapNotNull { raw ->
                val c = map(raw) ?: return@mapNotNull null
                val target = map(c["target"]) ?: c
                val id = int(target["id"]) ?: return@mapNotNull null
                NativeChat(
                    id = id,
                    type = int(target["type"]) ?: 0,
                    name = string(c["name"]).ifBlank { string((c["user_data"] as? Map<*, *>)?.get("name")) }.ifBlank { "Чат $id" },
                    avatar = string(c["avatar"]).ifBlank { null },
                    lastMessage = string(c["last_message"]),
                    notifications = int(c["notifications"]) ?: 0
                )
            }
            loaded = true
        }.onFailure { error = it.message ?: "Не удалось загрузить чаты" }
    }

    suspend fun loadMessages(chat: NativeChat) {
        loadingChat = true
        error = null
        runCatching {
            val response = socketClient.sendRequest(mapOf(
                "type" to "messenger",
                "action" to "load_messages",
                "target" to mapOf("id" to chat.id, "type" to chat.type)
            ))
            val rawMessages = response["messages"] as? List<*> ?: emptyList<Any?>()
            val parsed = mutableListOf<NativeMessage>()
            for (item in rawMessages) {
                val source = map(item) ?: continue
                val raw = source.toMutableMap()
                val encrypted = raw["encrypted"]
                if (encrypted != null) {
                    val decrypted = decrypt(encrypted)
                    if (decrypted != null) raw["decrypted"] = runCatching {
                        // The server stores the message body as JSON.
                        org.json.JSONObject(decrypted).let { obj ->
                            mapOf("text" to obj.optString("text"), "type" to obj.optString("type"), "is_edited" to obj.optBoolean("is_edited"))
                        }
                    }.getOrNull()
                }
                parseMessage(raw)?.let(parsed::add)
            }
            messages = parsed
            selected = chat
        }.onFailure { error = it.message ?: "Не удалось загрузить сообщения" }
        loadingChat = false
    }

    LaunchedEffect(socketClient, keyword) {
        if (keyword.isNotBlank()) loadChats()
    }

    LaunchedEffect(socketClient, selected?.id, selected?.type) {
        val chat = selected ?: return@LaunchedEffect
        if (keyword.isBlank()) return@LaunchedEffect
        loadMessages(chat)
    }

    LaunchedEffect(socketClient, selected?.id, selected?.type) {
        socketClient.events.collect { event ->
            if (event["type"]?.toString() != "messenger") return@collect
            val action = event["action"]?.toString().orEmpty()
            val target = map(event["target"])
            val chat = selected ?: return@collect
            val targetId = int(target?.get("id"))
            val targetType = int(target?.get("type"))
            if (targetId != null && (targetId != chat.id || targetType != chat.type)) return@collect
            when (action) {
                "new_message" -> {
                    val raw = when (val value = event["message"]) {
                        is Map<*, *> -> value.entries.associate { it.key.toString() to it.value }
                        is String -> runCatching {
                            val obj = org.json.JSONObject(value)
                            obj.keys().asSequence().associateWith { key -> obj.opt(key) }
                        }.getOrElse { event }
                        else -> event
                    }
                    val base = raw.toMutableMap()
                    val encrypted = base["encrypted"]
                    if (encrypted != null) {
                        val decrypted = decrypt(encrypted)
                        if (decrypted != null) {
                            base["decrypted"] = runCatching {
                                org.json.JSONObject(decrypted).let { obj ->
                                    mapOf("text" to obj.optString("text"), "is_edited" to obj.optBoolean("is_edited"))
                                }
                            }.getOrNull()
                        }
                    }
                    parseMessage(base)?.let { m -> messages = (messages + m).distinctBy { it.mid ?: it.tempMid } }
                }
                "message_deleted" -> {
                    val mid = long(event["mid"])
                    messages = messages.filterNot { it.mid == mid }
                }
                "message_edited" -> {
                    val mid = long(event["mid"])
                    val text = string((event["decrypted"] as? Map<*, *>)?.get("text"))
                    messages = messages.map { if (it.mid == mid) it.copy(text = text, edited = true) else it }
                }
            }
        }
    }

    suspend fun setKey() {
        val source = keyInput.trim()
        if (source.isBlank()) return
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(StandardCharsets.UTF_8))
        val derived = digest.joinToString("") { "%02x".format(it) }.take(32)
        val response = socketClient.sendRequest(mapOf("type" to "messenger", "action" to "aes_messages_key", "key" to derived))
        if (string(response["status"]).lowercase() != "success") {
            error = string(response["content"]).ifBlank { "Неверный секретный ключ" }
            return
        }
        keyword = string(response["keyword"]).ifBlank { derived }
        prefs.edit().putString("keyword", keyword).apply()
        showKeyDialog = false
        loadChats()
    }

    suspend fun sendMessage() {
        val chat = selected ?: return
        val text = messageText.trim()
        if (text.isBlank()) return
        val currentEdit = editing
        if (currentEdit?.mid != null) {
            val response = socketClient.sendRequest(mapOf(
                "type" to "messenger", "action" to "edit_message", "mid" to currentEdit.mid,
                "target" to mapOf("id" to chat.id, "type" to chat.type), "message" to text
            ))
            if (string(response["status"]).lowercase() in setOf("ok", "success")) {
                messages = messages.map { if (it.mid == currentEdit.mid) it.copy(text = text, edited = true) else it }
                editing = null; messageText = ""
            } else error = string(response["text"]).ifBlank { "Не удалось изменить сообщение" }
            return
        }
        val temp = (System.currentTimeMillis() % 1_000_000_000).toLong()
        socketClient.sendRequest(mapOf(
            "type" to "messenger", "action" to "send_message", "temp_mid" to temp,
            "target" to mapOf("id" to chat.id, "type" to chat.type), "message" to text
        ))
        messages = messages + NativeMessage(tempMid = temp, text = text, date = "Сейчас")
        messageText = ""
    }

    Column(modifier.fillMaxSize().background(ElementUiPalette.Body)) {
        if (selected == null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Мессенджер", fontSize = 24.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showKeyDialog = true }) { Icon(Icons.Default.Settings, "Ключ") }
            }
            if (!loaded) Text("Загрузка чатов…", Modifier.padding(20.dp))
            else if (chats.isEmpty()) Text("Чатов пока нет", Modifier.padding(20.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(chats, key = { "${it.type}:${it.id}" }) { chat ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = chat }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(46.dp).background(ElementUiPalette.Block, CircleShape), contentAlignment = Alignment.Center) {
                            Text(chat.name.take(1).uppercase())
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chat.name, fontSize = 17.sp)
                            if (chat.lastMessage.isNotBlank()) Text(chat.lastMessage, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                        }
                        if (chat.notifications > 0) Text(chat.notifications.toString())
                    }
                }
            }
        } else {
            val chat = selected!!
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selected = null; messages = emptyList() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                Text(chat.name, fontSize = 20.sp)
            }
            if (loadingChat) Text("Загрузка…", Modifier.padding(16.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(messages, key = { it.mid ?: it.tempMid ?: it.hashCode().toLong() }) { msg ->
                    val mine = msg.uid != null && accountId != null && msg.uid == accountId
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Column(Modifier.background(ElementUiPalette.Block, RoundedCornerShape(14.dp)).padding(10.dp).width(260.dp)) {
                            if (msg.author.isNotBlank() && !mine) Text(msg.author, fontSize = 12.sp)
                            Text(msg.text.ifBlank { "[медиа]" })
                            if (msg.edited) Text("изменено", fontSize = 10.sp, color = Color.Gray)
                            if (msg.mid != null) {
                                IconButton(onClick = {
                                    scope.launch {
                                        socketClient.sendRequest(mapOf(
                                            "type" to "messenger", "action" to "react_message", "mid" to msg.mid,
                                            "emoji" to "❤️", "target" to mapOf("id" to chat.id, "type" to chat.type)
                                        ))
                                    }
                                }) { Icon(Icons.Default.FavoriteBorder, "Реакция") }
                            }
                            if (mine && msg.mid != null) {
                                Row {
                                    IconButton(onClick = { editing = msg; messageText = msg.text }) { Icon(Icons.Default.Edit, "Изменить") }
                                    IconButton(onClick = {
                                        scope.launch {
                                            val response = socketClient.sendRequest(mapOf(
                                                "type" to "messenger", "action" to "delete_message", "mid" to msg.mid,
                                                "target" to mapOf("id" to chat.id, "type" to chat.type)
                                            ))
                                            if (string(response["status"]).lowercase() in setOf("ok", "success")) {
                                                messages = messages.filterNot { it.mid == msg.mid }
                                            } else {
                                                error = string(response["text"]).ifBlank { "Не удалось удалить сообщение" }
                                            }
                                        }
                                    }) { Icon(Icons.Default.Delete, "Удалить") }
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().imePadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(messageText, { messageText = it }, Modifier.weight(1f), placeholder = { Text("Сообщение") })
                IconButton(onClick = { scope.launch { sendMessage() } }) { Icon(Icons.Default.Send, "Отправить") }
            }
        }
    }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { if (keyword.isNotBlank()) showKeyDialog = false },
            title = { Text("Секретный ключ мессенджера") },
            text = { OutlinedTextField(keyInput, { keyInput = it }, visualTransformation = PasswordVisualTransformation(), label = { Text("Ключ") }) },
            confirmButton = { Button(onClick = { scope.launch { setKey() } }) { Text("Подключить") } },
            dismissButton = { if (keyword.isNotBlank()) TextButton(onClick = { showKeyDialog = false }) { Text("Отмена") } }
        )
    }
    error?.let { message ->
        AlertDialog(onDismissRequest = { error = null }, title = { Text("Мессенджер") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } })
    }
}
