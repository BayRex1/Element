package elemsocial.com.ui.pack.components.tabs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.ui.pack.theme.ElementUiPalette

@Composable
fun ElementSegmentTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tabs.isEmpty()) return
    val safeIndex = selectedIndex.takeIf { it in tabs.indices } ?: 0
    val shape = RoundedCornerShape(18.dp)
    val itemShape = RoundedCornerShape(14.dp)
    BoxWithConstraints(Modifier.fillMaxWidth().widthIn(max = 520.dp).height(44.dp).then(modifier)) {
        val pad = 4.dp
        val itemWidth = (maxWidth - pad * 2) / tabs.size
        val offset by animateDpAsState(pad + itemWidth * safeIndex, tween(220, easing = FastOutSlowInEasing), label = "segment_offset")
        Box(
            Modifier.fillMaxSize().clip(shape).background(ElementUiPalette.BlockSoft)
                .border(1.dp, ElementUiPalette.scaledBorder(0.8f), shape)
        )
        Box(
            Modifier.offset(offset, 4.dp).width(itemWidth).fillMaxHeight().padding(bottom = 4.dp)
                .clip(itemShape).background(ElementUiPalette.Block)
                .border(1.dp, ElementUiPalette.scaledBorder(1.05f), itemShape)
        )
        Row(Modifier.fillMaxSize().padding(horizontal = pad), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, title ->
                val selected = index == safeIndex
                val color by animateColorAsState(
                    if (selected) ElementUiPalette.TextPrimary else ElementUiPalette.TextSecondary,
                    tween(180), label = "segment_text"
                )
                val source = remember { MutableInteractionSource() }
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(itemShape)
                        .clickable(source, indication = null) { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(title, color = color, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}
