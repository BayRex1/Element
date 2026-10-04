package elemsocial.com.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import elemsocial.com.R

val ElementFontFamily = FontFamily(
    Font(R.font.sf_pro_display_medium, weight = FontWeight.Normal),
    Font(R.font.sf_pro_display_medium, weight = FontWeight.Medium),
    Font(R.font.sf_pro_display_bold, weight = FontWeight.SemiBold),
    Font(R.font.sf_pro_display_bold, weight = FontWeight.Bold)
)

private val BaseTypography = Typography()
private val WebBodyLetterSpacing = 0.019.em

private fun TextStyle.withElementFont(
    fontWeight: FontWeight = this.fontWeight ?: FontWeight.Normal,
    applyWebBodySpacing: Boolean = false
): TextStyle = copy(
    fontFamily = ElementFontFamily,
    fontWeight = fontWeight,
    letterSpacing = if (applyWebBodySpacing) WebBodyLetterSpacing else letterSpacing
)

val Typography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.withElementFont(fontWeight = FontWeight.Bold),
    displayMedium = BaseTypography.displayMedium.withElementFont(fontWeight = FontWeight.Bold),
    displaySmall = BaseTypography.displaySmall.withElementFont(fontWeight = FontWeight.Bold),
    headlineLarge = BaseTypography.headlineLarge.withElementFont(fontWeight = FontWeight.Bold),
    headlineMedium = BaseTypography.headlineMedium.withElementFont(fontWeight = FontWeight.Bold),
    headlineSmall = BaseTypography.headlineSmall.withElementFont(fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.withElementFont(fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.withElementFont(fontWeight = FontWeight.Bold),
    titleSmall = BaseTypography.titleSmall.withElementFont(fontWeight = FontWeight.Bold),
    bodyLarge = BaseTypography.bodyLarge.withElementFont(
        fontWeight = FontWeight.Normal,
        applyWebBodySpacing = true
    ),
    bodyMedium = BaseTypography.bodyMedium.withElementFont(
        fontWeight = FontWeight.Normal,
        applyWebBodySpacing = true
    ),
    bodySmall = BaseTypography.bodySmall.withElementFont(
        fontWeight = FontWeight.Normal,
        applyWebBodySpacing = true
    ),
    labelLarge = BaseTypography.labelLarge.withElementFont(
        fontWeight = FontWeight.Normal,
        applyWebBodySpacing = true
    ),
    labelMedium = BaseTypography.labelMedium.withElementFont(
        fontWeight = FontWeight.Normal,
        applyWebBodySpacing = true
    ),
    labelSmall = BaseTypography.labelSmall.withElementFont(
        fontWeight = FontWeight.Normal,
        applyWebBodySpacing = true
    )
)
