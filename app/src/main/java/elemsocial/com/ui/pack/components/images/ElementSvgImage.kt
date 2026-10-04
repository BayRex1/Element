package elemsocial.com.ui.pack.components.images

import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.caverock.androidsvg.SVG
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ElementSvgImage(
    svg: String,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    placeholderCornerRadius: Dp = 6.dp
) {
    val density = LocalDensity.current
    val widthPx = remember(width, density) { with(density) { width.roundToPx().coerceAtLeast(1) } }
    val heightPx = remember(height, density) { with(density) { height.roundToPx().coerceAtLeast(1) } }
    val bitmap by rememberElementSvgBitmap(svg = svg, widthPx = widthPx, heightPx = heightPx)

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            modifier = modifier.size(width = width, height = height),
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier
                .size(width = width, height = height)
                .clip(RoundedCornerShape(placeholderCornerRadius))
                .background(ElementUiPalette.BlockSoft)
        )
    }
}

@Composable
private fun rememberElementSvgBitmap(
    svg: String,
    widthPx: Int,
    heightPx: Int
) = produceState<ImageBitmap?>(initialValue = null, key1 = svg, key2 = widthPx, key3 = heightPx) {
    value = withContext(Dispatchers.Default) {
        runCatching {
            val svgDocument = SVG.getFromString(svg)
            val viewBox = svgDocument.documentViewBox
            if (viewBox == null) {
                val docWidth = svgDocument.documentWidth
                val docHeight = svgDocument.documentHeight
                if (docWidth > 0f && docHeight > 0f) {
                    svgDocument.setDocumentViewBox(0f, 0f, docWidth, docHeight)
                }
            }

            svgDocument.setDocumentWidth("100%")
            svgDocument.setDocumentHeight("100%")

            val picture = svgDocument.renderToPicture(widthPx, heightPx)
            val bitmap = android.graphics.Bitmap.createBitmap(
                widthPx,
                heightPx,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = AndroidCanvas(bitmap)
            canvas.drawPicture(
                picture,
                android.graphics.RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
            )
            bitmap.asImageBitmap()
        }.getOrNull()
    }
}
