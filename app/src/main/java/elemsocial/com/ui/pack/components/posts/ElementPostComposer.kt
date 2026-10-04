package elemsocial.com.ui.pack.components.posts

import android.graphics.BitmapFactory
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.domain.model.UploadFilePayload
import elemsocial.com.ui.pack.components.inputs.ElementInputField
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ElementPostComposer(
    text: String,
    isSending: Boolean,
    avatar: @Composable () -> Unit,
    onTextChange: (String) -> Unit,
    onEmojiClick: () -> Unit,
    onFileClick: () -> Unit,
    onMusicClick: () -> Unit,
    onPollClick: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    framed: Boolean = true,
    placeholder: String = "Текст поста...",
    inputTextFontSize: TextUnit = 16.sp,
    inputTextLineHeight: TextUnit = 20.sp,
    inputPlaceholderFontSize: TextUnit = 16.sp,
    inputMinHeight: Dp = 36.dp,
    inputHorizontalPadding: Dp = 12.dp,
    inputVerticalPadding: Dp = 8.dp,
    attachments: List<UploadFilePayload> = emptyList(),
    onRemoveAttachment: (UploadFilePayload) -> Unit = {},
    hasExtraContent: Boolean = false,
    showEmojiButton: Boolean = true,
    showImageSettingsButton: Boolean = false,
    showMusicButton: Boolean = true,
    showPollButton: Boolean = true,
    showAuthorAvatar: Boolean = true,
    imageSettingsExpanded: Boolean = false,
    onToggleImageSettings: () -> Unit = {},
    clearMetadataImage: Boolean = false,
    onClearMetadataImageChange: (Boolean) -> Unit = {},
    censoringImage: Boolean = false,
    onCensoringImageChange: (Boolean) -> Unit = {},
    sendButtonHeight: Dp = 30.dp,
    sendButtonMinWidth: Dp = 116.dp,
    sendButtonTextFontSize: TextUnit = 16.sp,
    actionButtonSize: Dp = 30.dp,
    actionIconSize: Dp = 17.dp
) {
    val canSend = (text.trim().isNotEmpty() || attachments.isNotEmpty() || hasExtraContent) && !isSending
    val removeAnimationScope = rememberCoroutineScope()
    val removingAttachments = remember { mutableStateListOf<String>() }

    LaunchedEffect(attachments) {
        val validKeys = attachments.map { composerAttachmentKey(it) }.toSet()
        val stale = removingAttachments.filterNot { it in validKeys }
        removingAttachments.removeAll(stale)
    }

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ElementInputField(
                value = text,
                onValueChange = onTextChange,
                placeholder = placeholder,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = inputMinHeight, max = 120.dp),
                singleLine = false,
                containerColor = ElementUiPalette.BlockSoft,
                borderColor = Color.Transparent,
                textFontSize = inputTextFontSize,
                textLineHeight = inputTextLineHeight,
                placeholderFontSize = inputPlaceholderFontSize,
                minHeight = inputMinHeight,
                contentHorizontalPadding = inputHorizontalPadding,
                contentVerticalPadding = inputVerticalPadding
            )

            if (attachments.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    attachments.forEachIndexed { index, file ->
                        val attachmentKey = composerAttachmentKey(file)
                        val isRemoving = removingAttachments.contains(attachmentKey)
                        key(attachmentKey + index) {
                            ComposerAttachmentCard(
                                file = file,
                                isRemoving = isRemoving,
                                onRemove = {
                                    if (isRemoving) return@ComposerAttachmentCard
                                    removingAttachments.add(attachmentKey)
                                    removeAnimationScope.launch {
                                        delay(120)
                                        onRemoveAttachment(file)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showImageSettingsButton && imageSettingsExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElementUiPalette.BlockSoft)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ComposerSwitchRow(
                        title = "Очистить метаданные",
                        value = clearMetadataImage,
                        onChange = onClearMetadataImageChange
                    )
                    ComposerSwitchRow(
                        title = "Деликатный контент",
                        value = censoringImage,
                        onChange = onCensoringImageChange
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showEmojiButton) {
                        ComposerActionButton(
                            icon = Icons.Default.SentimentSatisfied,
                            enabled = !isSending,
                            onClick = onEmojiClick,
                            buttonSize = actionButtonSize,
                            iconSize = actionIconSize
                        )
                    }
                    ComposerActionButton(
                        icon = Icons.Default.Description,
                        enabled = !isSending,
                        onClick = onFileClick,
                        buttonSize = actionButtonSize,
                        iconSize = actionIconSize
                    )
                    if (showImageSettingsButton) {
                        ComposerActionButton(
                            icon = Icons.Default.Settings,
                            enabled = !isSending,
                            active = imageSettingsExpanded,
                            onClick = onToggleImageSettings,
                            buttonSize = actionButtonSize,
                            iconSize = actionIconSize
                        )
                    }
                    if (showMusicButton) {
                        ComposerActionButton(
                            icon = Icons.Default.MusicNote,
                            enabled = !isSending,
                            onClick = onMusicClick,
                            buttonSize = actionButtonSize,
                            iconSize = actionIconSize
                        )
                    }
                    if (showPollButton) {
                        ComposerActionButton(
                            icon = ElementPollIcon,
                            enabled = !isSending,
                            onClick = onPollClick,
                            buttonSize = actionButtonSize,
                            iconSize = actionIconSize
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showAuthorAvatar) {
                        avatar()
                    }
                    Box(
                        modifier = Modifier
                            .height(sendButtonHeight)
                            .widthIn(min = sendButtonMinWidth)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElementUiPalette.Accent)
                            .clickable(enabled = canSend, onClick = onSend),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSending) "Отправка..." else "Отправить",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = sendButtonTextFontSize,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (framed) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
                .background(ElementUiPalette.Block)
                .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp)
        ) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

@Composable
private fun ComposerAttachmentCard(
    file: UploadFilePayload,
    isRemoving: Boolean,
    onRemove: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isRemoving) 0.82f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "attachment-scale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isRemoving) 0f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "attachment-alpha"
    )
    val isImage = file.mimeType.startsWith("image/")
    val isVideo = file.mimeType.startsWith("video/")
    val mediaLike = isImage || isVideo

    Box(
        modifier = Modifier
            .width(if (mediaLike) 160.dp else 200.dp)
            .height(120.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                alpha = animatedAlpha
            }
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.BlockSoft)
    ) {
        if (mediaLike) {
            val bitmap by rememberAttachmentPreviewBitmap(file)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = file.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                PlaceholderAttachment(isVideo = isVideo)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = ElementUiPalette.TextSecondary,
                    modifier = Modifier.size(40.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = file.name,
                        color = ElementUiPalette.TextPrimary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatFileSize(file.size.toLong()),
                        color = ElementUiPalette.TextLite,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (mediaLike) {
            if (isVideo) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatFileSize(file.size.toLong()),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(if (mediaLike) Color.Black.copy(alpha = 0.6f) else ElementUiPalette.Accent)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PlaceholderAttachment(isVideo: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(ElementUiPalette.BlockSoft),
        contentAlignment = Alignment.Center
    ) {
        if (isVideo) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = ElementUiPalette.TextLite.copy(alpha = 0.45f),
                    modifier = Modifier.size(26.dp)
                )
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = ElementUiPalette.TextLite,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun rememberAttachmentPreviewBitmap(file: UploadFilePayload) = produceState<ImageBitmap?>(
    initialValue = null,
    key1 = file.name,
    key2 = file.size,
    key3 = file.mimeType
) {
    val isImage = file.mimeType.startsWith("image/")
    val isVideo = file.mimeType.startsWith("video/")
    if (!isImage && !isVideo) {
        value = null
        return@produceState
    }

    if (isImage) {
        val bitmap = BitmapFactory.decodeByteArray(file.bytes, 0, file.bytes.size)
        value = bitmap?.asImageBitmap()
        return@produceState
    }

    val frame = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(ByteArrayMediaSource(file.bytes))
            retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } finally {
            retriever.release()
        }
    }.getOrNull()
    value = frame?.asImageBitmap()
}

private class ByteArrayMediaSource(
    private val bytes: ByteArray
) : MediaDataSource() {
    override fun getSize(): Long = bytes.size.toLong()

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= bytes.size) return -1
        val available = bytes.size - position.toInt()
        val read = minOf(size, available)
        System.arraycopy(bytes, position.toInt(), buffer, offset, read)
        return read
    }

    override fun close() = Unit
}

@Composable
private fun ComposerSwitchRow(
    title: String,
    value: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = ElementUiPalette.TextPrimary,
            fontSize = 13.sp
        )

        Row(
            modifier = Modifier
                .width(38.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(if (value) ElementUiPalette.Accent else ElementUiPalette.Border)
                .clickable { onChange(!value) }
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (value) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun ComposerActionButton(
    icon: ImageVector,
    enabled: Boolean,
    active: Boolean = false,
    onClick: () -> Unit,
    buttonSize: Dp = 30.dp,
    iconSize: Dp = 17.dp
) {
    val bg = when {
        !enabled -> ElementUiPalette.Interaction.copy(alpha = 0.55f)
        active -> ElementUiPalette.Accent.copy(alpha = 0.2f)
        else -> ElementUiPalette.Interaction
    }

    val tint = when {
        !enabled -> ElementUiPalette.TextSecondary.copy(alpha = 0.55f)
        active -> ElementUiPalette.Accent
        else -> ElementUiPalette.TextSecondary
    }

    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1_048_576) return String.format("%.2f KB", bytes / 1024.0)
    if (bytes < 1_073_741_824) return String.format("%.2f MB", bytes / 1_048_576.0)
    return String.format("%.2f GB", bytes / 1_073_741_824.0)
}

private fun composerAttachmentKey(file: UploadFilePayload): String {
    return buildString {
        append(file.name)
        append('|')
        append(file.mimeType)
        append('|')
        append(file.size)
        append('|')
        append(file.bytes.contentHashCode())
    }
}

private val ElementPollIcon: ImageVector = ImageVector.Builder(
    name = "ElementPollIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Black)
    ) {
        moveTo(5f, 9f)
        horizontalLineTo(7f)
        verticalLineTo(20f)
        horizontalLineTo(5f)
        verticalLineTo(9f)
        moveTo(9f, 4f)
        horizontalLineTo(11f)
        verticalLineTo(20f)
        horizontalLineTo(9f)
        verticalLineTo(4f)
        moveTo(13f, 11f)
        horizontalLineTo(15f)
        verticalLineTo(20f)
        horizontalLineTo(13f)
        verticalLineTo(11f)
        moveTo(17f, 8f)
        horizontalLineTo(19f)
        verticalLineTo(20f)
        horizontalLineTo(17f)
        verticalLineTo(8f)
    }
}.build()
