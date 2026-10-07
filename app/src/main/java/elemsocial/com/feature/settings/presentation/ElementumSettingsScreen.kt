package elemsocial.com.feature.settings.presentation

import androidx.annotation.DrawableRes
import android.provider.OpenableColumns
import android.widget.Toast
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayInputStream
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.R
import elemsocial.com.core.plugins.ElementPlugin
import elemsocial.com.core.plugins.ElementPluginStore
import elemsocial.com.feature.home.presentation.HomeGateway
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.theme.ElementUiPalette

@Composable
fun ElementumSettingsScreen(
    onBack: () -> Unit,
    homeGateway: HomeGateway? = null,
    modifier: Modifier = Modifier
) {
    var pluginsOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ElementUiPalette.Body)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Назад",
                    tint = ElementUiPalette.TextPrimary,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { rotationZ = 180f }
                )
            }

            Text(
                text = "Настройки Elementum",
                color = ElementUiPalette.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Основное",
                color = ElementUiPalette.TextLite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
            )

            UIKit.Block(
                modifier = Modifier.fillMaxWidth(),
                showShadow = false,
                contentPadding = 0.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ElementumMenuRow(
                        title = "Плагины",
                        subtitle = null,
                        iconRes = R.drawable.ic_settings_plugins,
                        onClick = { pluginsOpen = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (pluginsOpen) {
        ElementumPluginsModal(onClose = { pluginsOpen = false }, homeGateway = homeGateway)
    }
}

@Composable
private fun ElementumMenuRow(
    title: String,
    subtitle: String?,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ElementUiPalette.Accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = title,
                color = ElementUiPalette.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = ElementUiPalette.TextLite,
                    fontSize = 12.sp
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ElementUiPalette.TextLite,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ElementumPluginsModal(
    onClose: () -> Unit,
    homeGateway: HomeGateway? = null
) {
    val context = LocalContext.current
    val store = remember { ElementPluginStore(context) }
    var refreshKey by remember { mutableStateOf(0) }
    var settingsPlugin by remember { mutableStateOf<ElementPlugin?>(null) }
    val plugins = remember(refreshKey) { store.loadAll() }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val name = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                } else null
            }?.takeIf { it.isNotBlank() }
                ?: uri.lastPathSegment.orEmpty().substringAfterLast('/')

            require(name.lowercase(java.util.Locale.ROOT).endsWith(".plugin")) {
                "Выберите файл с расширением .plugin"
            }

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Не удалось прочитать файл")
            require(bytes.isNotEmpty()) { "Файл плагина пуст" }
            store.savePlugin(bytes, name).getOrThrow()
        }.onSuccess {
            refreshKey++
            Toast.makeText(context, "Плагин «${it.name}» установлен", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Ошибка плагина: ${it.message ?: "неверный файл"}", Toast.LENGTH_LONG).show()
        }
    }

    UIKit.RoutedModal(
        title = "Плагины",
        onClose = onClose,
        trailing = {
            androidx.compose.material3.IconButton(onClick = { picker.launch(arrayOf("*/*")) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить плагин",
                    tint = ElementUiPalette.TextPrimary
                )
            }
        }
    ) {
        if (plugins.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Плагинов пока нет", color = ElementUiPalette.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Нажмите + в правом верхнем углу и выберите .plugin", color = ElementUiPalette.TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                plugins.forEach { plugin ->
                    UIKit.Block(modifier = Modifier.fillMaxWidth(), showShadow = false) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(ElementUiPalette.BlockSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                var iconBitmap by remember(plugin.id, plugin.icon, plugin.iconBase64) {
                                    mutableStateOf(
                                        plugin.iconBase64?.let { encoded ->
                                            runCatching {
                                                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                            }.getOrNull()
                                        }
                                    )
                                }
                                LaunchedEffect(plugin.id, plugin.icon, homeGateway) {
                                    if (iconBitmap == null && homeGateway != null && plugin.icon.startsWith("post/")) {
                                        val postId = plugin.icon.removePrefix("post/").trim().toIntOrNull()
                                        if (postId != null) {
                                            val loaded: ImageBitmap? = runCatching {
                                                homeGateway.loadPost(postId).post?.content?.images?.firstOrNull()?.asset
                                            }.getOrNull()?.let { asset ->
                                                runCatching { homeGateway.loadImageBitmap(asset) }.getOrNull()
                                            }
                                            if (loaded != null) iconBitmap = loaded
                                        }
                                    }
                                }
                                val bitmap = iconBitmap
                                if (bitmap != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bitmap,
                                        contentDescription = plugin.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(plugin.icon.ifBlank { "🧩" }, fontSize = 24.sp)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(plugin.name, color = ElementUiPalette.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                if (plugin.description.isNotBlank()) {
                                    Text(plugin.description, color = ElementUiPalette.TextSecondary, fontSize = 12.sp, maxLines = 2)
                                }
                                Text("v${plugin.version} · ${plugin.author}", color = ElementUiPalette.TextLite, fontSize = 11.sp)
                            }

                            androidx.compose.material3.IconButton(onClick = { settingsPlugin = plugin }) {
                                Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = ElementUiPalette.TextSecondary)
                            }
                            androidx.compose.material3.IconButton(onClick = {
                                store.delete(plugin)
                                refreshKey++
                                Toast.makeText(context, "Плагин удалён", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = ElementUiPalette.Error)
                            }
                        }
                    }
                }
            }
        }
    }

    settingsPlugin?.let { plugin ->
        AlertDialog(
            onDismissRequest = { settingsPlugin = null },
            title = { Text(plugin.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(plugin.description.ifBlank { "Настройки отсутствуют" })
                    if (plugin.settings.isNotEmpty()) {
                        plugin.settings.forEach { (key, value) ->
                            Text("$key: $value", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { settingsPlugin = null }) { Text("Готово") } }
        )
    }
}
