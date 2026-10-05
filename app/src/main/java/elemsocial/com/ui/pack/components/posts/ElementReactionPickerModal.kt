package elemsocial.com.ui.pack.components.posts

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import elemsocial.com.domain.model.PostReactions
import elemsocial.com.ui.pack.components.reactions.EmojiCatalog
import elemsocial.com.ui.pack.components.reactions.EmojiItem
import elemsocial.com.ui.pack.theme.ElementUiPalette

@Composable
fun ElementReactionPickerModal(
    currentReactions: PostReactions,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val emojis = remember { EmojiCatalog.load(context) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ElementUiPalette.Block)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Реакция",
                        color = ElementUiPalette.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = ElementUiPalette.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    items(emojis, key = { it.unified }) { emoji ->
                        val isSet = currentReactions.userReactions
                            .any { it.equals(emoji.unified, ignoreCase = true) }
                        EmojiCell(
                            emoji = emoji,
                            selected = isSet,
                            onClick = {
                                onSelect(emoji.unified)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiCell(
    emoji: EmojiItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val assetDir = remember { EmojiCatalog.assetDir() }
    val assetBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = emoji.unified) {
        value = runCatching {
            val name = "$assetDir/${emoji.unified}.webp"
            context.assets.open(name).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) ElementUiPalette.Accent.copy(alpha = 0.20f)
                else ElementUiPalette.BlockSoft
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) ElementUiPalette.Accent.copy(alpha = 0.45f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (assetBitmap != null) {
            Image(
                bitmap = assetBitmap!!,
                contentDescription = emoji.emoji,
                modifier = Modifier.size(28.dp)
            )
        } else {
            Text(
                text = emoji.emoji,
                fontSize = 24.sp
            )
        }
    }
}
