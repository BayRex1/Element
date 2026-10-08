package elemsocial.com.ui.pack.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import elemsocial.com.core.settings.AppThemeMode
import elemsocial.com.core.plugins.ElementPluginThemeOverrides

private data class ElementPalette(
    val body: Color,
    val block: Color,
    val blockSoft: Color,
    val interaction: Color,
    val interactionText: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textLite: Color,
    val border: Color,
    val error: Color,
    val success: Color,
    val info: Color
)

private val LightPalette = ElementPalette(
    body = Color(0xFFF2F1F6),
    block = Color(0xFFFEFEFE),
    blockSoft = Color(0xFFF3F2F6),
    interaction = Color(0xFFF4F3F6),
    interactionText = Color(0xA6534C5D),
    accent = Color(0xFF995AF6),
    textPrimary = Color(0xFF514E58),
    textSecondary = Color(0xFF615D6A),
    textLite = Color(0x808B879A),
    border = Color(0x4DC3BFD1),
    error = Color(0xFFDC4D63),
    success = Color(0xFF2CA58D),
    info = Color(0xFF5578B5)
)

private val DarkPalette = ElementPalette(
    body = Color(0xFF151515),
    block = Color(0xFF252525),
    blockSoft = Color(0xFF3B3B3B),
    interaction = Color(0xB33D3D3D),
    interactionText = Color(0xB3FFFFFF),
    accent = Color(0xFF995AF6),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFBFBFBF),
    textLite = Color(0xFFBDBDBE),
    border = Color(0x338F8F8F),
    error = Color(0xFFDC4D63),
    success = Color(0xFF2CA58D),
    info = Color(0xFF5578B5)
)

private val AmoledPalette = ElementPalette(
    body = Color(0xFF000000),
    block = Color(0xFF0F0F0F),
    blockSoft = Color(0xFF222222),
    interaction = Color(0xB3282828),
    interactionText = Color(0xB3FFFFFF),
    accent = Color(0xFF995AF6),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFBFBFBF),
    textLite = Color(0xFFBDBDBE),
    border = Color(0x802D2D2D),
    error = Color(0xFFDC4D63),
    success = Color(0xFF2CA58D),
    info = Color(0xFF5578B5)
)

object ElementUiPalette {
    private var activePalette by mutableStateOf(LightPalette)
    private var pluginOverrides by mutableStateOf<ElementPluginThemeOverrides?>(null)

    private fun pluginColor(value: Int?): Color? = value?.let(::Color)

    val Body: Color
        get() = pluginColor(pluginOverrides?.bodyArgb) ?: activePalette.body

    val Block: Color
        get() = pluginColor(pluginOverrides?.blockArgb) ?: activePalette.block

    val BlockSoft: Color
        get() = pluginColor(pluginOverrides?.blockSoftArgb) ?: activePalette.blockSoft

    val Interaction: Color
        get() = activePalette.interaction

    val InteractionText: Color
        get() = activePalette.interactionText

    val Accent: Color
        get() = pluginColor(pluginOverrides?.accentArgb) ?: activePalette.accent

    val TextPrimary: Color
        get() = pluginColor(pluginOverrides?.textPrimaryArgb) ?: activePalette.textPrimary

    val TextSecondary: Color
        get() = pluginColor(pluginOverrides?.textSecondaryArgb) ?: activePalette.textSecondary

    val TextLite: Color
        get() = activePalette.textLite

    val Border: Color
        get() = activePalette.border

    fun scaledBorder(alphaMultiplier: Float): Color {
        return activePalette.border.copy(
            alpha = (activePalette.border.alpha * alphaMultiplier).coerceIn(0f, 1f)
        )
    }

    val Error: Color
        get() = pluginColor(pluginOverrides?.errorArgb) ?: activePalette.error

    val Success: Color
        get() = pluginColor(pluginOverrides?.successArgb) ?: activePalette.success

    val Info: Color
        get() = pluginColor(pluginOverrides?.infoArgb) ?: activePalette.info

    fun applyTheme(mode: AppThemeMode) {
        activePalette = when (mode) {
            AppThemeMode.Dark -> DarkPalette
            AppThemeMode.Amoled -> AmoledPalette
            AppThemeMode.Light,
            AppThemeMode.System -> LightPalette
        }
    }

    fun applyPluginOverrides(overrides: ElementPluginThemeOverrides) {
        pluginOverrides = overrides
    }

    fun clearPluginOverrides() {
        pluginOverrides = null
    }
}
