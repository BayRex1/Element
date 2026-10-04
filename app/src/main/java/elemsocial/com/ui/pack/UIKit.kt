package elemsocial.com.ui.pack

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.core.model.LocalTransparencyMode
import elemsocial.com.core.model.TransparencyMode
import elemsocial.com.core.model.glassAlphaFor
import elemsocial.com.core.model.glassSoftAlphaFor
import elemsocial.com.core.model.shouldUseGlassBlur
import elemsocial.com.ui.pack.components.animations.ElementLikeBurst
import elemsocial.com.ui.pack.components.base.ElementBlock
import elemsocial.com.ui.pack.components.base.ElementEball
import elemsocial.com.ui.pack.components.buttons.ElementButton
import elemsocial.com.ui.pack.components.buttons.ElementButtonVariant
import elemsocial.com.ui.pack.components.buttons.ElementIconButton
import elemsocial.com.ui.pack.components.buttons.ElementInteractionButton
import elemsocial.com.ui.pack.components.buttons.ElementPlainIconButton
import elemsocial.com.ui.pack.components.feedback.ElementLoader
import elemsocial.com.ui.pack.components.feedback.ElementGiftSkeleton
import elemsocial.com.ui.pack.components.feedback.ElementHallRowSkeleton
import elemsocial.com.ui.pack.components.feedback.ElementNotificationSkeleton
import elemsocial.com.ui.pack.components.feedback.ElementPostSkeleton
import elemsocial.com.ui.pack.components.feedback.ElementProfileRelationSkeleton
import elemsocial.com.ui.pack.components.feedback.ElementRowSkeleton
import elemsocial.com.ui.pack.components.feedback.ElementWalletTransactionSkeleton
import elemsocial.com.ui.pack.components.inputs.ElementInputField
import elemsocial.com.ui.pack.components.navigation.ElementBottomNav
import elemsocial.com.ui.pack.components.navigation.ElementBottomNavItem
import elemsocial.com.ui.pack.components.modals.ElementRoutedModal
import elemsocial.com.ui.pack.components.modals.ElementContextMenu
import elemsocial.com.ui.pack.components.modals.ElementContextMenuItem
import elemsocial.com.ui.pack.components.tabs.ElementSegmentTabs
import elemsocial.com.ui.pack.theme.ElementUiPalette

object UIKit {
    @Composable
    fun PlainIconButton(
        icon: ImageVector,
        onClick: () -> Unit
    ) {
        ElementPlainIconButton(
            icon = icon,
            onClick = onClick
        )
    }

    @Composable
    fun IconButton(
        icon: ImageVector,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
    ) {
        ElementIconButton(
            icon = icon,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled
        )
    }

    @Composable
    fun Block(
        modifier: Modifier = Modifier,
        showShadow: Boolean = false,
        contentPadding: Dp = 10.dp,
        content: @Composable () -> Unit
    ) {
        ElementBlock(
            modifier = modifier,
            showShadow = showShadow,
            contentPadding = contentPadding
        ) {
            content()
        }
    }

    @Composable
    fun Button(
        title: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        loading: Boolean = false,
        variant: ElementButtonVariant = ElementButtonVariant.Primary,
        textFontWeight: FontWeight? = null
    ) {
        ElementButton(
            title = title,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            loading = loading,
            variant = variant,
            textFontWeight = textFontWeight
        )
    }

    @Composable
    fun Eball(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        fontSize: TextUnit = 12.sp
    ) {
        ElementEball(
            modifier = modifier,
            size = size,
            fontSize = fontSize
        )
    }

    @Composable
    fun Input(
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
        ElementInputField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = modifier,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            borderWidth = borderWidth,
            borderColor = borderColor,
            containerColor = containerColor,
            textColor = textColor,
            textFontSize = textFontSize,
            textLineHeight = textLineHeight,
            placeholderFontSize = placeholderFontSize,
            shape = shape,
            minHeight = minHeight,
            contentHorizontalPadding = contentHorizontalPadding,
            contentVerticalPadding = contentVerticalPadding
        )
    }

    @Composable
    fun Loader(
        modifier: Modifier = Modifier,
        centered: Boolean = false,
        size: Int = 24
    ) {
        ElementLoader(
            modifier = modifier,
            centered = centered,
            size = size
        )
    }

    @Composable
    fun SkeletonRow(
        modifier: Modifier = Modifier
    ) {
        ElementRowSkeleton(modifier = modifier)
    }

    @Composable
    fun SkeletonNotification(
        modifier: Modifier = Modifier
    ) {
        ElementNotificationSkeleton(modifier = modifier)
    }

    @Composable
    fun SkeletonRelation(
        modifier: Modifier = Modifier
    ) {
        ElementProfileRelationSkeleton(modifier = modifier)
    }

    @Composable
    fun SkeletonWalletTransaction(
        modifier: Modifier = Modifier
    ) {
        ElementWalletTransactionSkeleton(modifier = modifier)
    }

    @Composable
    fun SkeletonHallRow(
        modifier: Modifier = Modifier
    ) {
        ElementHallRowSkeleton(modifier = modifier)
    }

    @Composable
    fun SkeletonPost(
        modifier: Modifier = Modifier,
        mediaHeight: Dp = 190.dp,
        showMedia: Boolean = true
    ) {
        ElementPostSkeleton(
            modifier = modifier,
            mediaHeight = mediaHeight,
            showMedia = showMedia
        )
    }

    @Composable
    fun SkeletonGift(
        modifier: Modifier = Modifier
    ) {
        ElementGiftSkeleton(modifier = modifier)
    }

    @Composable
    fun InteractionButton(
        icon: ImageVector,
        active: Boolean,
        enabled: Boolean,
        count: Int? = null,
        text: String? = null,
        shape: RoundedCornerShape,
        activeTintColor: Color = ElementUiPalette.Accent,
        activeBackgroundColor: Color = activeTintColor.copy(alpha = 0.2f),
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        ElementInteractionButton(
            icon = icon,
            active = active,
            enabled = enabled,
            count = count,
            text = text,
            shape = shape,
            activeTintColor = activeTintColor,
            activeBackgroundColor = activeBackgroundColor,
            modifier = modifier,
            onClick = onClick
        )
    }

    @Composable
    fun SegmentTabs(
        tabs: List<String>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        ElementSegmentTabs(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = modifier
        )
    }

    @Composable
    fun BottomNav(
        items: List<ElementBottomNavItem>,
        selectedIndex: Int,
        onSelect: (Int) -> Unit,
        modifier: Modifier = Modifier,
        showShadow: Boolean = false
    ) {
        ElementBottomNav(
            items = items,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            modifier = modifier,
            showShadow = showShadow
        )
    }

    @Composable
    fun LikeBurst(
        trigger: Int,
        origin: Offset? = null,
        modifier: Modifier = Modifier
    ) {
        ElementLikeBurst(
            trigger = trigger,
            origin = origin,
            modifier = modifier
        )
    }

    @Composable
    fun RoutedModal(
        title: String,
        onClose: () -> Unit,
        modifier: Modifier = Modifier,
        trailing: @Composable (() -> Unit)? = null,
        headerBackgroundColor: Color = ElementUiPalette.Body,
        headerHeight: Dp = 55.dp,
        contentHorizontalPadding: Dp = 10.dp,
        titleFontWeight: FontWeight = FontWeight.SemiBold,
        content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
    ) {
        ElementRoutedModal(
            title = title,
            onClose = onClose,
            modifier = modifier,
            trailing = trailing,
            headerBackgroundColor = headerBackgroundColor,
            headerHeight = headerHeight,
            contentHorizontalPadding = contentHorizontalPadding,
            titleFontWeight = titleFontWeight,
            transparencyMode = LocalTransparencyMode.current,
            content = content
        )
    }

    @Composable
    fun ContextMenu(
        expanded: Boolean,
        anchorBounds: IntRect?,
        items: List<ElementContextMenuItem>,
        onDismissRequest: () -> Unit,
        modifier: Modifier = Modifier,
        minWidth: Dp = 200.dp
    ) {
        ElementContextMenu(
            expanded = expanded,
            anchorBounds = anchorBounds,
            items = items,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            minWidth = minWidth
        )
    }

    @Composable
    fun DropdownMenu(
        expanded: Boolean,
        onDismissRequest: () -> Unit,
        modifier: Modifier = Modifier,
        minWidth: Dp = 200.dp,
        maxWidth: Dp = 260.dp,
        content: @Composable ColumnScope.() -> Unit
    ) {
        val transparencyMode = LocalTransparencyMode.current
        val opaque = transparencyMode == TransparencyMode.OPAQUE
        val menuAlpha = if (opaque) 1f else glassAlphaFor(transparencyMode)
        val menuSoftAlpha = if (opaque) 1f else glassSoftAlphaFor(transparencyMode)
        val blurEnabled = shouldUseGlassBlur(transparencyMode)
        val menuBrush = Brush.verticalGradient(
            colors = listOf(
                ElementUiPalette.Block.copy(alpha = menuAlpha),
                if (opaque) {
                    ElementUiPalette.Block.copy(alpha = menuSoftAlpha)
                } else {
                    ElementUiPalette.BlockSoft.copy(alpha = menuSoftAlpha)
                }
            )
        )
        val menuShape = RoundedCornerShape(22.dp)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier
                .widthIn(min = minWidth, max = maxWidth)
                .clip(menuShape),
            shape = menuShape,
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = null
        ) {
            Box(
                modifier = Modifier
                    .clip(menuShape)
            ) {
                if (blurEnabled) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(22.dp)
                            .background(brush = menuBrush)
                    )
                }

                Column(
                    modifier = Modifier
                        .clip(menuShape)
                        .background(brush = menuBrush)
                        .border(BorderStroke(1.dp, ElementUiPalette.scaledBorder(0.9f)), menuShape)
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    content = content
                )
            }
        }
    }

    @Composable
    fun DropdownMenuItem(
        text: String,
        onClick: () -> Unit,
        icon: ImageVector? = null,
        destructive: Boolean = false,
        enabled: Boolean = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (destructive) {
                        ElementUiPalette.Error.copy(alpha = 0.15f)
                    } else {
                        Color.Transparent
                    }
                )
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (destructive) ElementUiPalette.Error else ElementUiPalette.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = text,
                    color = if (!enabled) {
                        ElementUiPalette.TextPrimary.copy(alpha = 0.45f)
                    } else if (destructive) {
                        ElementUiPalette.Error
                    } else {
                        ElementUiPalette.TextPrimary
                    },
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
