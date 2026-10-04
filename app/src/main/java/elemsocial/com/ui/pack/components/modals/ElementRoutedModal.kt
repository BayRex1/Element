package elemsocial.com.ui.pack.components.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import elemsocial.com.core.model.TransparencyMode
import elemsocial.com.core.model.dialogBackgroundBlurRadiusFor
import elemsocial.com.core.model.dialogBlurBehindRadiusFor
import elemsocial.com.core.model.glassAlphaFor
import elemsocial.com.core.model.shouldUseGlassBlur
import elemsocial.com.ui.pack.theme.ElementUiPalette

@Composable
fun ElementRoutedModal(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    headerBackgroundColor: Color = ElementUiPalette.Body,
    headerHeight: Dp = 55.dp,
    contentHorizontalPadding: Dp = 10.dp,
    titleFontWeight: FontWeight = FontWeight.SemiBold,
    transparencyMode: TransparencyMode = TransparencyMode.ADAPTIVE,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        ElementDialogBlurEffect(
            enabled = shouldUseGlassBlur(transparencyMode),
            backgroundBlurRadius = dialogBackgroundBlurRadiusFor(transparencyMode),
            blurBehindRadius = dialogBlurBehindRadiusFor(transparencyMode)
        )

        val backdropAlpha = when (transparencyMode) {
            TransparencyMode.TRANSPARENT -> 0.15f
            TransparencyMode.OPAQUE -> 0.40f
            TransparencyMode.ADAPTIVE -> 0.28f
        }
        val modalSurfaceAlpha = (glassAlphaFor(transparencyMode) + 0.08f).coerceAtMost(1f)

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backdropAlpha))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onClose
                    )
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val mobile = maxWidth < 768.dp
                val modalShape = if (mobile) {
                    RoundedCornerShape(0.dp)
                } else {
                    RoundedCornerShape(16.dp)
                }
                val modalModifier = if (mobile) {
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxSize()
                } else {
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .fillMaxHeight(0.96f)
                        .padding(vertical = 12.dp)
                }

                Box(
                    modifier = modalModifier
                        .fillMaxSize()
                        .clip(modalShape)
                        .background(ElementUiPalette.Body.copy(alpha = modalSurfaceAlpha))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {}
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val headerSurfaceColor = if (transparencyMode == TransparencyMode.OPAQUE) {
                            headerBackgroundColor.copy(alpha = modalSurfaceAlpha)
                        } else {
                            Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .height(headerHeight)
                                .background(headerSurfaceColor)
                        ) {
                            IconButton(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 6.dp),
                                onClick = onClose
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Назад",
                                    tint = ElementUiPalette.TextPrimary
                                )
                            }

                            Text(
                                text = title,
                                color = ElementUiPalette.TextPrimary,
                                fontWeight = titleFontWeight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 56.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 10.dp)
                                    .height(36.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                trailing?.invoke()
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = contentHorizontalPadding)
                                .navigationBarsPadding()
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}
