package elemsocial.com.ui.pack.components.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import elemsocial.com.ui.pack.theme.ElementUiPalette

@Composable
fun ElementBlock(
    modifier: Modifier = Modifier,
    showShadow: Boolean = false,
    contentPadding: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.Block)
            .padding(contentPadding),
        content = content
    )
}
