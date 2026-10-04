package elemsocial.com.ui.pack.components.inputs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import elemsocial.com.ui.pack.theme.ElementUiPalette

@Composable
fun ElementInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    containerColor: Color = ElementUiPalette.BlockSoft,
    textColor: Color = ElementUiPalette.TextPrimary,
    textFontSize: TextUnit = 16.sp,
    textLineHeight: TextUnit = 20.sp,
    placeholderFontSize: TextUnit = 16.sp,
    shape: Shape = RoundedCornerShape(10.dp),
    minHeight: Dp = 40.dp,
    contentHorizontalPadding: Dp = 12.dp,
    contentVerticalPadding: Dp = 8.dp
) {
    val maxLines = if (singleLine) 1 else Int.MAX_VALUE
    val style = TextStyle(
        color = textColor,
        fontWeight = FontWeight.Normal,
        fontSize = textFontSize,
        lineHeight = textLineHeight
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        singleLine = singleLine,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        cursorBrush = SolidColor(ElementUiPalette.Accent),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(shape)
            .background(containerColor, shape)
            .border(width = borderWidth, color = borderColor, shape = shape),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = ElementUiPalette.TextSecondary,
                            fontSize = placeholderFontSize,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    innerTextField()
                }

                if (trailingIcon != null) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .width(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        trailingIcon()
                    }
                }
            }
        }
    )
}
