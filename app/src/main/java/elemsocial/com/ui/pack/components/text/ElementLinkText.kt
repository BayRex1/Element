package elemsocial.com.ui.pack.components.text

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import elemsocial.com.ui.pack.theme.ElementUiPalette

private val LinkRegex = Regex("""((https?://|www\.)[^\s]+)""", RegexOption.IGNORE_CASE)

@Composable
fun ElementLinkText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onDoubleTap: ((Offset) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val annotated = buildLinkAnnotatedString(
        source = text,
        base = style,
        link = style.copy(
            color = ElementUiPalette.Accent,
            textDecoration = TextDecoration.Underline
        )
    )
    var textLayout by remember(annotated) { mutableStateOf<TextLayoutResult?>(null) }

    BasicText(
        text = annotated,
        style = style,
        modifier = modifier.pointerInput(annotated, onDoubleTap) {
            detectTapGestures(
                onTap = { position ->
                    val offset = textLayout?.getOffsetForPosition(position) ?: return@detectTapGestures
                    annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()
                        ?.item
                        ?.let { raw ->
                            val target = if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) {
                                raw
                            } else {
                                "https://$raw"
                            }
                            runCatching { uriHandler.openUri(target) }
                        }
                },
                onDoubleTap = { offset ->
                    onDoubleTap?.invoke(offset)
                }
            )
        },
        onTextLayout = { textLayout = it }
    )
}

private fun buildLinkAnnotatedString(
    source: String,
    base: TextStyle,
    link: TextStyle
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        for (match in LinkRegex.findAll(source)) {
            val start = match.range.first
            val endExclusive = match.range.last + 1
            if (cursor < start) {
                withStyle(base.toSpanStyle()) {
                    append(source.substring(cursor, start))
                }
            }

            val raw = source.substring(start, endExclusive)
            val clean = raw.trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}')
            val trimmedTail = raw.length - clean.length
            if (clean.isNotBlank()) {
                pushStringAnnotation(tag = "URL", annotation = clean)
                withStyle(link.toSpanStyle()) {
                    append(clean)
                }
                pop()
            }
            if (trimmedTail > 0) {
                withStyle(base.toSpanStyle()) {
                    append(raw.takeLast(trimmedTail))
                }
            }
            cursor = endExclusive
        }

        if (cursor < source.length) {
            withStyle(base.toSpanStyle()) {
                append(source.substring(cursor))
            }
        }
    }
}
