package elemsocial.com.feature.main.presentation

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.core.ws.ElementSocketClient
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private data class NativeChat(
    val id: Int,
    val type: Int,
    val name: String,
    val avatar: String? = null,
    val lastMessage: String = "",
    val notifications: Int = 0,
    val online: Boolean = false
)

private data class NativeMessage(
    val mid: Long? = null,
    val tempMid: Long? = null,
    val uid: Int? = null,
    val author: String = "",
    val text: String = "",
    val type: String = "text",
    val fileName: String? = null,
    val previewBase64: String? = null,
    val duration: Int = 0,
    val date: String = "",
    val edited: Boolean = false,
    val reactions: Map<String, Int> = emptyMap()
)

private val MessengerAccent = Color(0xFFFFB51B)
private val MessengerBackground = Color(0xFF101010)
private val MessengerSurface = Color(0xFF202020)
private val MessengerIncoming = Color(0xFF171717)

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
    var loadingOlder by remember { mutableStateOf(false) }
    var hasOlder by remember { mutableStateOf(true) }
    var messageText by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<NativeMessage?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showKeyDialog by remember { mutableStateOf(keyword.isBlank()) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun map(value: Any?): Map<String, Any?>? = when (value) {
        is Map<*, *> -> value.entries.associate { it.key.toString() to it.value }
        else -> null
    }

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
        is Map<*, *> -> value.entries
            .sortedWith(compareBy({ it.key?.toString()?.toIntOrNull() ?: Int.MAX_VALUE }, { it.key?.toString().orEmpty() }))
            .mapNotNull { int(it.value)?.toByte() }
            .toByteArray()
        else -> null
    }

    suspend fun decrypt(value: Any?): String? = withContext(Dispatchers.Default) {
        if (value == null) return@withContext null
        if (value is String) {
            val trimmed = value.trim()
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) return@withContext trimmed
            if (keyword.length == 32) {
                val decoded = runCatching { Base64.decode(trimmed, Base64.DEFAULT) }.getOrNull()
                if (decoded != null && decoded.size > 16) {
                    runCatching {
                        val key = SecretKeySpec(keyword.toByteArray(StandardCharsets.UTF_8), "AES")
                        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(decoded.copyOfRange(0, 16)))
                        return@withContext String(cipher.doFinal(decoded.copyOfRange(16, decoded.size)), StandardCharsets.UTF_8)
                    }
                }
            }
            return@withContext trimmed
        }

        val encrypted = bytes(value) ?: return@withContext null
        if (keyword.length != 32 || encrypted.size <= 16) return@withContext null
        runCatching {
            val key = SecretKeySpec(keyword.toByteArray(StandardCharsets.UTF_8), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(encrypted.copyOfRange(0, 16)))
            String(cipher.doFinal(encrypted.copyOfRange(16, encrypted.size)), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    fun jsonMap(text: String): Map<String, Any?>? {
        val obj = runCatching { JSONObject(text) }.getOrNull() ?: return null
        fun convert(value: Any?): Any? = when (value) {
            is JSONObject -> value.keys().asSequence().associateWith { key -> convert(value.opt(key)) }
            is JSONArray -> (0 until value.length()).map { convert(value.opt(it)) }
            JSONObject.NULL -> null
            else -> value
        }
        return convert(obj) as? Map<String, Any?>
    }

    fun parseMessage(raw: Map<String, Any?>): NativeMessage? {
        val decrypted = map(raw["decrypted"])
        val file = map(decrypted?.get("file"))
        val preview = map(decrypted?.get("preview"))
        val type = string(decrypted?.get("type")).ifBlank { "text" }
        val text = string(decrypted?.get("text")).ifBlank {
            when (type) {
                "image" -> "Фото"
                "voice" -> "Голосовое сообщение"
                "video" -> "Видеосообщение"
                "file" -> string(file?.get("name")).ifBlank { "Файл" }
                "call" -> "Звонок"
                else -> string(raw["message"])
            }
        }
        val reactions = map(raw["reactions"])?.mapNotNull { (emoji, value) ->
            int(value)?.let { emoji to it }
        }?.toMap().orEmpty()
        return NativeMessage(
            mid = long(raw["mid"]),
            tempMid = long(raw["temp_mid"]),
            uid = int(raw["uid"]),
            author = string(map(raw["author"])?.get("name")),
            text = text,
            type = type,
            fileName = string(file?.get("name")).ifBlank { null },
            previewBase64 = string(preview?.get("base64")).ifBlank {
                string(file?.get("base64")).ifBlank { null }
            },
            duration = int(file?.get("duration")) ?: 0,
            date = string(raw["date"]),
            edited = decrypted?.get("is_edited") == true,
            reactions = reactions
        )
    }

    fun parseChats(raw: Any?): List<NativeChat> {
        val list = raw as? List<*> ?: return emptyList()
        return list.mapNotNull { item ->
            val c = map(item) ?: return@mapNotNull null
            val target = map(c["target"]) ?: c
            val id = int(target["id"]) ?: return@mapNotNull null
            val user = map(c["user_data"])
            NativeChat(
                id = id,
                type = int(target["type"]) ?: int(c["type"]) ?: 0,
                name = string(c["name"]).ifBlank { string(user?.get("name")) }.ifBlank { "Чат $id" },
                avatar = string(c["avatar"]).ifBlank { string(user?.get("avatar")).ifBlank { null } },
                lastMessage = string(c["last_message"]),
                notifications = int(c["notifications"]) ?: 0,
                online = c["online"] == true || user?.get("online") == true
            )
        }
    }

    suspend fun loadChats() {
        runCatching {
            val response = socketClient.sendRequest(mapOf("type" to "messenger", "action" to "load_chats"))
            chats = parseChats(response["chats"])
            loaded = true
        }.onFailure { error = it.message ?: "Не удалось загрузить чаты" }
    }

    suspend fun decodeMessages(rawMessages: List<*>): List<NativeMessage> {
        val parsed = ArrayList<NativeMessage>(rawMessages.size)
        for (item in rawMessages) {
            val source = map(item) ?: continue
            val raw = source.toMutableMap()
            if (raw["encrypted"] != null) {
                val decrypted = decrypt(raw["encrypted"])
                if (decrypted != null) raw["decrypted"] = jsonMap(decrypted)
            }
            parseMessage(raw)?.let(parsed::add)
        }
        return parsed.sortedBy { it.date }
    }

    suspend fun loadMessages(chat: NativeChat, older: Boolean = false) {
        if (older) {
            if (loadingOlder || !hasOlder) return
            loadingOlder = true
        } else {
            loadingChat = true
        }
        error = null
        runCatching {
            val startIndex = if (older) messages.size else 0
            val response = socketClient.sendRequest(
                mapOf(
                    "type" to "messenger",
                    "action" to "load_messages",
                    "target" to mapOf("id" to chat.id, "type" to chat.type),
                    "startIndex" to startIndex
                )
            )
            val rawMessages = response["messages"] as? List<*> ?: emptyList<Any?>()
            val parsed = decodeMessages(rawMessages)
            if (older) {
                val merged = (parsed + messages).distinctBy { it.mid ?: it.tempMid }
                hasOlder = parsed.isNotEmpty() && parsed.size >= 25
                messages = merged.sortedBy { it.date }
            } else {
                messages = parsed
                hasOlder = parsed.size >= 25
                selected = chat
            }
        }.onFailure { error = it.message ?: "Не удалось загрузить сообщения" }
        loadingChat = false
        loadingOlder = false
    }

    LaunchedEffect(socketClient, keyword) {
        if (keyword.isNotBlank()) loadChats()
    }

    LaunchedEffect(selected?.id, selected?.type, keyword) {
        val chat = selected ?: return@LaunchedEffect
        if (keyword.isBlank()) return@LaunchedEffect
        loadMessages(chat)
    }

    LaunchedEffect(selected?.id, selected?.type) {
        socketClient.events.collect { event ->
            if (event["type"]?.toString() != "messenger") return@collect
            val action = event["action"]?.toString().orEmpty()
            val chat = selected ?: return@collect
            val target = map(event["target"])
            val targetId = int(target?.get("id"))
            val targetType = int(target?.get("type"))
            if (targetId != null && (targetId != chat.id || targetType != chat.type)) return@collect

            when (action) {
                "new_message" -> {
                    val raw = when (val value = event["message"]) {
                        is Map<*, *> -> value.entries.associate { it.key.toString() to it.value }
                        is String -> runCatching { jsonMap(value) }.getOrNull()
                        else -> null
                    }?.toMutableMap() ?: return@collect
                    raw["uid"] = raw["uid"] ?: event["uid"]
                    raw["author"] = raw["author"] ?: event["author"]
                    raw["date"] = raw["date"] ?: event["date"]
                    if (raw["decrypted"] == null && (raw["text"] != null || raw["type"] != null)) {
                        raw["decrypted"] = raw.toMap()
                    }
                    parseMessage(raw)?.let { incoming ->
                        messages = (messages + incoming).distinctBy { it.mid ?: it.tempMid }.sortedBy { it.date }
                    }
                }
                "message_deleted" -> {
                    val mid = long(event["mid"])
                    messages = messages.filterNot { it.mid == mid }
                }
                "message_edited" -> {
                    val mid = long(event["mid"])
                    val decrypted = map(event["decrypted"])
                    val text = string(decrypted?.get("text"))
                    if (text.isNotBlank()) messages = messages.map { if (it.mid == mid) it.copy(text = text, edited = true) else it }
                }
                "message_reaction" -> {
                    val mid = long(event["mid"]) ?: return@collect
                    val emoji = string(event["emoji"]).ifBlank { "❤️" }
                    val count = int(event["count"]) ?: 1
                    messages = messages.map { msg ->
                        if (msg.mid != mid) msg else msg.copy(reactions = msg.reactions + (emoji to count))
                    }
                }
            }
        }
    }

    LaunchedEffect(listState, selected?.id) {
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index <= 1 && selected != null && !loadingChat) {
                    loadMessages(selected!!, older = true)
                }
            }
    }

    suspend fun setKey() {
        val source = keyInput.trim()
        if (source.isBlank()) return
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(StandardCharsets.UTF_8))
        val derived = digest.joinToString("") { "%02x".format(it) }.take(32)
        runCatching {
            val response = socketClient.sendRequest(
                mapOf("type" to "messenger", "action" to "aes_messages_key", "key" to derived)
            )
            if (string(response["status"]).lowercase() != "success") {
                error = string(response["content"]).ifBlank { string(response["message"]).ifBlank { "Неверный секретный ключ" } }
                return@runCatching
            }
            keyword = string(response["keyword"]).ifBlank { derived }
            prefs.edit().putString("keyword", keyword).apply()
            showKeyDialog = false
            loadChats()
        }.onFailure { error = it.message ?: "Не удалось подключить мессенджер" }
    }

    suspend fun sendMessage() {
        val chat = selected ?: return
        val text = messageText.trim()
        if (text.isBlank()) return
        val currentEdit = editing
        if (currentEdit?.mid != null) {
            val response = socketClient.sendRequest(
                mapOf(
                    "type" to "messenger", "action" to "edit_message", "mid" to currentEdit.mid,
                    "target" to mapOf("id" to chat.id, "type" to chat.type), "message" to text
                )
            )
            if (string(response["status"]).lowercase() in setOf("ok", "success")) {
                messages = messages.map { if (it.mid == currentEdit.mid) it.copy(text = text, edited = true) else it }
                editing = null
                messageText = ""
            } else error = string(response["text"]).ifBlank { string(response["message"]).ifBlank { "Не удалось изменить сообщение" } }
            return
        }
        val temp = (System.currentTimeMillis() % 1_000_000_000).toLong()
        val response = socketClient.sendRequest(
            mapOf(
                "type" to "messenger", "action" to "send_message", "temp_mid" to temp,
                "target" to mapOf("id" to chat.id, "type" to chat.type), "message" to text
            )
        )
        val sentMid = long(response["mid"])
        messages = messages + NativeMessage(
            mid = sentMid,
            tempMid = if (sentMid == null) temp else null,
            uid = accountId,
            text = text,
            date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(Date())
        )
        messageText = ""
    }

    val visibleMessages = if (searchText.isBlank()) messages else messages.filter {
        it.text.contains(searchText, ignoreCase = true) || it.fileName?.contains(searchText, ignoreCase = true) == true
    }

    Column(modifier.fillMaxSize().background(MessengerBackground)) {
        if (selected == null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Мессенджер", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showKeyDialog = true }) { Icon(Icons.Default.Settings, "Ключ", tint = Color.White) }
            }
            if (!loaded) Text("Загрузка чатов…", Modifier.padding(20.dp), color = Color.Gray)
            else if (chats.isEmpty()) Text("Чатов пока нет", Modifier.padding(20.dp), color = Color.Gray)
            LazyColumn(Modifier.fillMaxSize()) {
                items(chats, key = { "${it.type}:${it.id}" }) { chat ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = chat }.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(name = chat.name, avatar = chat.avatar, size = 54.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(chat.name, color = Color.White, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (chat.lastMessage.isNotBlank()) {
                                Text(chat.lastMessage, color = Color(0xFF9D9D9D), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (chat.notifications > 0) {
                            Box(Modifier.size(22.dp).background(MessengerAccent, CircleShape), contentAlignment = Alignment.Center) {
                                Text(chat.notifications.toString(), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            val chat = selected!!
            Row(
                Modifier.fillMaxWidth().background(MessengerSurface).padding(horizontal = 6.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selected = null; messages = emptyList(); searchText = "" }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White)
                }
                Avatar(name = chat.name, avatar = chat.avatar, size = 44.dp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(chat.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (chat.online) "в сети" else "не в сети", color = if (chat.online) MessengerAccent else Color(0xFF9D9D9D), fontSize = 12.sp)
                }
                if (searchOpen) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.width(150.dp),
                        singleLine = true,
                        placeholder = { Text("Поиск", color = Color.Gray) }
                    )
                    IconButton(onClick = { searchOpen = false; searchText = "" }) { Icon(Icons.Default.Close, "Закрыть", tint = Color.White) }
                } else {
                    IconButton(onClick = { searchOpen = true }) { Icon(Icons.Default.Search, "Поиск", tint = Color.White) }
                    IconButton(onClick = { /* call protocol is wired separately */ }) { Icon(Icons.Default.Call, "Позвонить", tint = Color.White) }
                    IconButton(onClick = { /* video call protocol is wired separately */ }) { Icon(Icons.Default.Videocam, "Видео", tint = Color.White) }
                }
                IconButton(onClick = { /* chat menu */ }) { Icon(Icons.Default.MoreVert, "Меню", tint = Color.White) }
            }

            if (editing != null) {
                Row(Modifier.fillMaxWidth().background(Color(0xFF28231A)).padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, tint = MessengerAccent, modifier = Modifier.size(18.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text("Редактирование", color = MessengerAccent, fontSize = 12.sp)
                        Text(editing?.text.orEmpty(), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                    }
                    IconButton(onClick = { editing = null; messageText = "" }) { Icon(Icons.Default.Close, "Отмена", tint = Color.White) }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                reverseLayout = false
            ) {
                if (loadingOlder) item { Text("Загрузка…", Modifier.fillMaxWidth().padding(8.dp), color = Color.Gray, fontSize = 12.sp) }
                var previousDate: String? = null
                items(visibleMessages, key = { it.mid ?: it.tempMid ?: it.hashCode().toLong() }) { msg ->
                    val dateLabel = formatDayLabel(msg.date)
                    if (dateLabel != previousDate) {
                        previousDate = dateLabel
                        Box(Modifier.fillMaxWidth().padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                            Text(dateLabel, Modifier.background(Color(0xFF171717), RoundedCornerShape(18.dp)).padding(horizontal = 15.dp, vertical = 5.dp), color = Color(0xFFE8E8E8), fontSize = 14.sp)
                        }
                    }
                    MessageBubble(
                        message = msg,
                        mine = msg.uid != null && accountId != null && msg.uid == accountId,
                        onReact = {
                            val mid = msg.mid
                            if (mid != null) {
                                scope.launch {
                                    socketClient.sendRequest(mapOf("type" to "messenger", "action" to "react_message", "mid" to mid, "emoji" to "❤️", "target" to mapOf("id" to chat.id, "type" to chat.type)))
                                }
                            }
                        },
                        onEdit = { editing = msg; messageText = msg.text },
                        onDelete = {
                            val mid = msg.mid
                            if (mid != null) scope.launch {
                                val response = socketClient.sendRequest(mapOf("type" to "messenger", "action" to "delete_message", "mid" to mid, "target" to mapOf("id" to chat.id, "type" to chat.type)))
                                if (string(response["status"]).lowercase() in setOf("ok", "success")) messages = messages.filterNot { it.mid == mid }
                                else error = string(response["text"]).ifBlank { "Не удалось удалить сообщение" }
                            }
                        }
                    )
                }
            }

            Row(Modifier.fillMaxWidth().imePadding().padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.Bottom) {
                RoundAction(Icons.Default.Add, "Вложение")
                Box(Modifier.weight(1f).padding(horizontal = 7.dp).border(1.dp, Color(0xFF343434), RoundedCornerShape(28.dp)).background(MessengerSurface, RoundedCornerShape(28.dp))) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Сообщение", color = Color(0xFF9D9D9D)) },
                            singleLine = false,
                            maxLines = 4,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MessengerAccent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        IconButton(onClick = { }) { Icon(Icons.Default.EmojiEmotions, "Эмодзи", tint = Color(0xFFD0D0D0)) }
                    }
                }
                if (messageText.isBlank()) {
                    RoundAction(Icons.Default.Mic, "Голосовое", accent = true)
                } else {
                    RoundAction(Icons.Default.Send, "Отправить", accent = true, onClick = { scope.launch { sendMessage() } })
                }
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

@Composable
private fun Avatar(name: String, avatar: String?, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(0xFF9D89C7)), contentAlignment = Alignment.Center) {
        Text(name.firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = (size.value * .42f).sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, accent: Boolean = false, onClick: () -> Unit = {}) {
    IconButton(onClick = onClick, modifier = Modifier.size(52.dp).background(if (accent) MessengerAccent else MessengerSurface, CircleShape)) {
        Icon(icon, description, tint = if (accent) Color.Black else Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun MessageBubble(
    message: NativeMessage,
    mine: Boolean,
    onReact: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            if (!mine && message.author.isNotBlank()) {
                Text(message.author, color = MessengerAccent, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 1.dp))
            }
            Box(
                Modifier
                    .width(if (message.type == "image") 300.dp else 0.dp)
                    .let { if (message.type == "image") it else Modifier }
                    .clip(RoundedCornerShape(18.dp, 18.dp, if (mine) 5.dp else 18.dp, if (mine) 5.dp else 18.dp))
                    .background(if (mine) MessengerAccent else MessengerIncoming)
                    .clickable(enabled = message.mid != null) { onReact() }
                    .padding(if (message.type == "image" && message.previewBase64 != null) 0.dp else 11.dp)
            ) {
                Column {
                    MediaContent(message)
                    if (message.type == "text" && message.text.isNotBlank()) {
                        Text(message.text, color = if (mine) Color.Black else Color.White, fontSize = 17.sp, lineHeight = 21.sp)
                    } else if (message.type != "image" && message.text.isNotBlank()) {
                        Text(message.text, color = if (mine) Color.Black else Color.White, fontSize = 16.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMessageTime(message.date), color = if (mine) Color.Black.copy(alpha = .5f) else Color.Gray, fontSize = 11.sp)
                        if (mine) Icon(Icons.Default.DoneAll, null, tint = Color.Black.copy(alpha = .45f), modifier = Modifier.size(17.dp).padding(start = 2.dp))
                    }
                }
            }
            if (message.reactions.isNotEmpty()) {
                Row(Modifier.padding(top = 2.dp, start = 8.dp, end = 8.dp).background(Color(0xFF29251A), RoundedCornerShape(13.dp)).padding(horizontal = 7.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    message.reactions.forEach { (emoji, count) ->
                        Text("$emoji $count", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
            if (mine && message.mid != null) {
                Row(horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Edit, "Изменить", tint = Color.Gray, modifier = Modifier.size(15.dp)) }
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Delete, "Удалить", tint = Color.Gray, modifier = Modifier.size(15.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MediaContent(message: NativeMessage) {
    when (message.type) {
        "image" -> {
            val bitmap by rememberBase64Bitmap(message.previewBase64)
            if (bitmap != null) {
                Image(bitmap = bitmap!!, contentDescription = "Изображение", modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(17.dp)), contentScale = ContentScale.Crop)
            } else {
                Text("Фото", color = Color.White, fontSize = 16.sp)
            }
        }
        "voice" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Default.Mic, null, tint = if (message.uid != null) Color.Black else Color.White, modifier = Modifier.size(24.dp))
            Text(if (message.duration > 0) "Голосовое · ${formatDuration(message.duration)}" else "Голосовое сообщение", color = if (message.uid != null) Color.Black else Color.White, fontSize = 15.sp)
        }
        "video" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Text(if (message.duration > 0) "Видеосообщение · ${formatDuration(message.duration)}" else "Видеосообщение", color = Color.White, fontSize = 15.sp)
        }
        "file" -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Default.AttachFile, null, tint = Color.White, modifier = Modifier.size(24.dp))
            Text(message.fileName ?: "Файл", color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun rememberBase64Bitmap(base64: String?) = produceState<ImageBitmap?>(initialValue = null, key1 = base64) {
    if (base64.isNullOrBlank()) return@produceState
    value = withContext(Dispatchers.Default) {
        runCatching {
            val raw = base64.substringAfter("base64,", base64)
            val bytes = Base64.decode(raw, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
}

private fun formatMessageTime(value: String): String {
    val date = parseDate(value) ?: return value.takeLast(5)
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

private fun formatDayLabel(value: String): String {
    val date = parseDate(value) ?: return ""
    return SimpleDateFormat("d MMMM", Locale("ru")).format(date)
}

private fun parseDate(value: String): Date? {
    if (value.isBlank()) return null
    val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSX", "yyyy-MM-dd'T'HH:mm:ssX", "yyyy-MM-dd HH:mm:ss")
    for (pattern in patterns) {
        runCatching { return SimpleDateFormat(pattern, Locale.US).parse(value) }.getOrNull()
    }
    return null
}

private fun formatDuration(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
