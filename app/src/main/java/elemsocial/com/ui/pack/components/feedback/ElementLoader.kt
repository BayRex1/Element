package elemsocial.com.ui.pack.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import elemsocial.com.ui.pack.theme.ElementUiPalette

@Composable
fun ElementLoader(
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    size: Int = 24
) {
    if (centered) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(size.dp),
                strokeWidth = 2.dp,
                color = ElementUiPalette.Accent
            )
        }
        return
    }

    CircularProgressIndicator(
        modifier = modifier.size(size.dp),
        strokeWidth = 2.dp,
        color = ElementUiPalette.Accent
    )
}
