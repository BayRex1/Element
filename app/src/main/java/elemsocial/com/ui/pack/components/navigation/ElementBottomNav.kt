package elemsocial.com.ui.pack.components.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.core.model.LocalTransparencyMode
import elemsocial.com.core.model.glassAlphaFor
import elemsocial.com.core.model.glassSoftAlphaFor
import elemsocial.com.core.model.shouldUseGlassBlur
import elemsocial.com.ui.pack.theme.ElementUiPalette

data class ElementBottomNavItem(
    val title: String,
    @DrawableRes val iconRes: Int,
    val badgeCount: Int? = null
)

@Composable
fun ElementBottomNav(
    items: List<ElementBottomNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showShadow: Boolean = false
) {
    if (items.isEmpty()) return

    val containerShape = RoundedCornerShape(30.dp)
    val activeShape = RoundedCornerShape(22.dp)
    val hasSelection = selectedIndex in items.indices
    val safeSelectedIndex = selectedIndex.takeIf { it in items.indices } ?: 0
    val transparencyMode = LocalTransparencyMode.current
    val containerAlpha = glassAlphaFor(transparencyMode)
    val containerSoftAlpha = glassSoftAlphaFor(transparencyMode)
    val blurEnabled = shouldUseGlassBlur(transparencyMode)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp)
            .padding(bottom = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .height(68.dp)
        ) {
            val rowHorizontalPadding = 6.dp
            val contentWidth = maxWidth - (rowHorizontalPadding * 2)
            val segmentWidth = contentWidth / items.size
            val sliderWidth = segmentWidth
            val sliderOffset by animateDpAsState(
                targetValue = rowHorizontalPadding + (segmentWidth * safeSelectedIndex),
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                label = "bottom_nav_slider_offset"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = if (showShadow) 18.dp else 0.dp,
                        shape = containerShape,
                        ambientColor = ElementUiPalette.TextPrimary.copy(alpha = 0.10f),
                        spotColor = ElementUiPalette.TextPrimary.copy(alpha = 0.14f)
                    )
                    .clip(containerShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                ElementUiPalette.Block.copy(alpha = containerAlpha),
                                ElementUiPalette.BlockSoft.copy(alpha = containerSoftAlpha)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = ElementUiPalette.scaledBorder(0.85f),
                        shape = containerShape
                    )
            ) {
                if (blurEnabled) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(22.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        ElementUiPalette.Block.copy(alpha = containerAlpha),
                                        ElementUiPalette.BlockSoft.copy(alpha = containerSoftAlpha)
                                    )
                                )
                            )
                    )
                }
            }

            if (hasSelection) {
                Box(
                    modifier = Modifier
                        .offset(x = sliderOffset, y = 6.dp)
                        .width(sliderWidth)
                        .height(56.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = activeShape,
                            ambientColor = ElementUiPalette.Accent.copy(alpha = 0.16f),
                            spotColor = ElementUiPalette.Accent.copy(alpha = 0.20f)
                        )
                        .clip(activeShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ElementUiPalette.Block.copy(alpha = containerAlpha),
                                    ElementUiPalette.Accent.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = ElementUiPalette.scaledBorder(1.15f),
                            shape = activeShape
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = rowHorizontalPadding, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val selected = hasSelection && index == selectedIndex
                    val interactionSource = remember { MutableInteractionSource() }
                    val iconColor by animateColorAsState(
                        targetValue = if (selected) {
                            ElementUiPalette.Accent
                        } else {
                            ElementUiPalette.TextPrimary.copy(alpha = 0.72f)
                        },
                        animationSpec = tween(durationMillis = 220),
                        label = "bottom_nav_icon_color"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (selected) {
                            ElementUiPalette.TextPrimary
                        } else {
                            ElementUiPalette.TextPrimary.copy(alpha = 0.68f)
                        },
                        animationSpec = tween(durationMillis = 220),
                        label = "bottom_nav_text_color"
                    )
                    val count = item.badgeCount ?: 0

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(activeShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onSelect(index) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(30.dp)
                                    .height(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = item.title,
                                    tint = iconColor,
                                    modifier = Modifier.size(22.dp)
                                )

                                if (count > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-4).dp)
                                            .background(ElementUiPalette.Accent, CircleShape)
                                            .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                                            .padding(horizontal = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = count.coerceAtMost(99).toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            lineHeight = 9.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Text(
                                text = item.title,
                                color = textColor,
                                fontSize = 9.sp,
                                lineHeight = 9.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
