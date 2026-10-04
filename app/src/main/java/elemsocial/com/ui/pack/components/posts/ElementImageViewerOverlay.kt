package elemsocial.com.ui.pack.components.posts

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import elemsocial.com.domain.model.PostImage
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.ui.pack.UIKit
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

private enum class ViewerDragMode {
    None,
    Horizontal,
    Vertical
}

private const val VIEWER_MIN_SCALE = 1f
private const val VIEWER_MAX_SCALE = 4f
private const val VIEWER_SCALE_EPSILON = 0.02f
private const val VIEWER_DOUBLE_TAP_SCALE = 2.75f

@Composable
fun ElementImageViewerOverlay(
    images: List<PostImage>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    loadImageBytes: suspend (PostImageAsset) -> ByteArray?
) {
    if (images.isEmpty()) return

    val safeStart = initialIndex.coerceIn(0, images.lastIndex)
    var selectedIndex by remember(images, safeStart) { mutableIntStateOf(safeStart) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    val bitmapCache = remember { mutableStateMapOf<String, ImageBitmap>() }

    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }
    val dismissDragYAnim = remember { Animatable(0f) }
    val pagerDragXAnim = remember { Animatable(0f) }
    var dragMode by remember { mutableStateOf(ViewerDragMode.None) }
    var chromeVisible by remember { mutableStateOf(true) }

    val selectedImage = images.getOrNull(selectedIndex)
    val selectedBitmap by rememberViewerBitmap(
        asset = selectedImage?.asset,
        loadImageBytes = loadImageBytes,
        bitmapCache = bitmapCache
    )
    val previousImage = images.getOrNull(selectedIndex - 1)
    val previousBitmap by rememberViewerBitmap(
        asset = previousImage?.asset,
        loadImageBytes = loadImageBytes,
        bitmapCache = bitmapCache
    )
    val nextImage = images.getOrNull(selectedIndex + 1)
    val nextBitmap by rememberViewerBitmap(
        asset = nextImage?.asset,
        loadImageBytes = loadImageBytes,
        bitmapCache = bitmapCache
    )

    val viewportWidth = viewportSize.width.toFloat().coerceAtLeast(1f)
    val viewportHeight = viewportSize.height.toFloat().coerceAtLeast(1f)
    val scale = scaleAnim.value
    val offsetX = offsetXAnim.value
    val offsetY = offsetYAnim.value
    val dismissDragY = dismissDragYAnim.value
    val pagerDragX = pagerDragXAnim.value
    val isZoomed = scale > 1f + VIEWER_SCALE_EPSILON
    val dismissProgress = (abs(dismissDragY) / (viewportHeight * 0.33f)).coerceIn(0f, 1f)
    val backgroundAlpha = (0.96f - dismissProgress * 0.72f).coerceIn(0.18f, 0.96f)
    val imageAlpha = (1f - dismissProgress * 0.14f).coerceIn(0.86f, 1f)
    val dismissScale = if (!isZoomed) {
        (1f - dismissProgress * 0.08f).coerceAtLeast(0.92f)
    } else {
        1f
    }
    val activeTranslationX = if (isZoomed) offsetX else pagerDragX
    val activeTranslationY = if (isZoomed) offsetY else dismissDragY

    LaunchedEffect(selectedIndex) {
        scaleAnim.snapTo(1f)
        offsetXAnim.snapTo(0f)
        offsetYAnim.snapTo(0f)
        dismissDragYAnim.snapTo(0f)
        pagerDragXAnim.snapTo(0f)
        dragMode = ViewerDragMode.None
        chromeVisible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backgroundAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 20.dp)
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(selectedIndex, viewportSize, selectedBitmap) {
                        detectTapGestures(
                            onTap = { chromeVisible = !chromeVisible },
                            onDoubleTap = { tap ->
                                scope.launch {
                                    if (scaleAnim.value > 1.05f) {
                                        dragMode = ViewerDragMode.None
                                        chromeVisible = true
                                        animateViewerState(
                                            scale = scaleAnim,
                                            offsetX = offsetXAnim,
                                            offsetY = offsetYAnim,
                                            dismissDragY = dismissDragYAnim,
                                            pagerDragX = pagerDragXAnim,
                                            targetScale = 1f,
                                            targetOffsetX = 0f,
                                            targetOffsetY = 0f,
                                            targetDismissY = 0f,
                                            targetPagerX = 0f
                                        )
                                    } else {
                                        val targetScale = VIEWER_DOUBLE_TAP_SCALE
                                        val focusX = tap.x - viewportWidth / 2f
                                        val focusY = tap.y - viewportHeight / 2f
                                        val rawOffsetX = -focusX * (targetScale - 1f)
                                        val rawOffsetY = -focusY * (targetScale - 1f)
                                        val (maxX, maxY) = viewerMaxOffsets(
                                            containerWidth = viewportWidth,
                                            containerHeight = viewportHeight,
                                            bitmap = selectedBitmap,
                                            scale = targetScale
                                        )

                                        dragMode = ViewerDragMode.None
                                        chromeVisible = false
                                        animateViewerState(
                                            scale = scaleAnim,
                                            offsetX = offsetXAnim,
                                            offsetY = offsetYAnim,
                                            dismissDragY = dismissDragYAnim,
                                            pagerDragX = pagerDragXAnim,
                                            targetScale = targetScale,
                                            targetOffsetX = rawOffsetX.coerceIn(-maxX, maxX),
                                            targetOffsetY = rawOffsetY.coerceIn(-maxY, maxY),
                                            targetDismissY = 0f,
                                            targetPagerX = 0f
                                        )
                                    }
                                }
                            }
                        )
                    }
                    .pointerInput(selectedIndex, viewportSize, selectedBitmap) {
                        detectTransformGesturesWithEnd(
                            onGesture = { centroid, pan, zoom, pointerCount ->
                                scope.launch {
                                    scaleAnim.stop()
                                    offsetXAnim.stop()
                                    offsetYAnim.stop()
                                    dismissDragYAnim.stop()
                                    pagerDragXAnim.stop()

                                    val prevScale = scaleAnim.value
                                    val nextScale = (prevScale * zoom).coerceIn(
                                        VIEWER_MIN_SCALE,
                                        VIEWER_MAX_SCALE
                                    )
                                    val zooming = abs(zoom - 1f) > 0.0015f
                                    val transformMode = pointerCount > 1 ||
                                        zooming ||
                                        prevScale > 1f + VIEWER_SCALE_EPSILON ||
                                        nextScale > 1f + VIEWER_SCALE_EPSILON

                                    if (transformMode) {
                                        val zoomFactor = if (prevScale <= 0f) 1f else nextScale / prevScale
                                        val focusX = centroid.x - viewportWidth / 2f
                                        val focusY = centroid.y - viewportHeight / 2f
                                        val proposedOffsetX = offsetXAnim.value + pan.x + focusX * (1f - zoomFactor)
                                        val proposedOffsetY = offsetYAnim.value + pan.y + focusY * (1f - zoomFactor)

                                        val activeScale = if (nextScale <= 1f + VIEWER_SCALE_EPSILON) 1f else nextScale
                                        scaleAnim.snapTo(activeScale)
                                        if (activeScale <= 1f + VIEWER_SCALE_EPSILON) {
                                            offsetXAnim.snapTo(0f)
                                            offsetYAnim.snapTo(0f)
                                        } else {
                                            val (maxX, maxY) = viewerMaxOffsets(
                                                containerWidth = viewportWidth,
                                                containerHeight = viewportHeight,
                                                bitmap = selectedBitmap,
                                                scale = activeScale
                                            )
                                            offsetXAnim.snapTo(proposedOffsetX.coerceIn(-maxX, maxX))
                                            offsetYAnim.snapTo(proposedOffsetY.coerceIn(-maxY, maxY))
                                        }

                                        dismissDragYAnim.snapTo(0f)
                                        pagerDragXAnim.snapTo(0f)
                                        dragMode = ViewerDragMode.None
                                        return@launch
                                    }

                                    if (dragMode == ViewerDragMode.None) {
                                        dragMode = if (abs(pan.x) > abs(pan.y)) {
                                            ViewerDragMode.Horizontal
                                        } else {
                                            ViewerDragMode.Vertical
                                        }
                                    }

                                    when (dragMode) {
                                        ViewerDragMode.Horizontal -> {
                                            val atLeftEdge = selectedIndex == 0 && pan.x > 0f
                                            val atRightEdge = selectedIndex == images.lastIndex && pan.x < 0f
                                            val friction = if (atLeftEdge || atRightEdge) 0.32f else 1f
                                            pagerDragXAnim.snapTo(pagerDragXAnim.value + pan.x * friction)
                                            dismissDragYAnim.snapTo(0f)
                                        }

                                        ViewerDragMode.Vertical -> {
                                            dismissDragYAnim.snapTo(dismissDragYAnim.value + pan.y)
                                            pagerDragXAnim.snapTo(0f)
                                        }

                                        ViewerDragMode.None -> Unit
                                    }
                                }
                            },
                            onGestureEnd = {
                                scope.launch {
                                    if (scaleAnim.value > 1f + VIEWER_SCALE_EPSILON) {
                                        val (maxX, maxY) = viewerMaxOffsets(
                                            containerWidth = viewportWidth,
                                            containerHeight = viewportHeight,
                                            bitmap = selectedBitmap,
                                            scale = scaleAnim.value
                                        )
                                        animateViewerState(
                                            scale = scaleAnim,
                                            offsetX = offsetXAnim,
                                            offsetY = offsetYAnim,
                                            dismissDragY = dismissDragYAnim,
                                            pagerDragX = pagerDragXAnim,
                                            targetScale = scaleAnim.value,
                                            targetOffsetX = offsetXAnim.value.coerceIn(-maxX, maxX),
                                            targetOffsetY = offsetYAnim.value.coerceIn(-maxY, maxY),
                                            targetDismissY = 0f,
                                            targetPagerX = 0f
                                        )
                                        dragMode = ViewerDragMode.None
                                        return@launch
                                    }

                                    when (dragMode) {
                                        ViewerDragMode.Horizontal -> {
                                            val switchThreshold = viewportWidth * 0.16f
                                            when {
                                                pagerDragXAnim.value <= -switchThreshold && selectedIndex < images.lastIndex -> {
                                                    animateViewerPageSwitch(
                                                        direction = 1,
                                                        viewportWidth = viewportWidth,
                                                        scale = scaleAnim,
                                                        offsetX = offsetXAnim,
                                                        offsetY = offsetYAnim,
                                                        dismissDragY = dismissDragYAnim,
                                                        pagerDragX = pagerDragXAnim,
                                                        onIndexChanged = { selectedIndex += 1 }
                                                    )
                                                }

                                                pagerDragXAnim.value >= switchThreshold && selectedIndex > 0 -> {
                                                    animateViewerPageSwitch(
                                                        direction = -1,
                                                        viewportWidth = viewportWidth,
                                                        scale = scaleAnim,
                                                        offsetX = offsetXAnim,
                                                        offsetY = offsetYAnim,
                                                        dismissDragY = dismissDragYAnim,
                                                        pagerDragX = pagerDragXAnim,
                                                        onIndexChanged = { selectedIndex -= 1 }
                                                    )
                                                }

                                                else -> {
                                                    animateViewerState(
                                                        scale = scaleAnim,
                                                        offsetX = offsetXAnim,
                                                        offsetY = offsetYAnim,
                                                        dismissDragY = dismissDragYAnim,
                                                        pagerDragX = pagerDragXAnim,
                                                        targetScale = 1f,
                                                        targetOffsetX = 0f,
                                                        targetOffsetY = 0f,
                                                        targetDismissY = 0f,
                                                        targetPagerX = 0f
                                                    )
                                                }
                                            }
                                        }

                                        ViewerDragMode.Vertical -> {
                                            val dismissThreshold = viewportHeight * 0.17f
                                            if (abs(dismissDragYAnim.value) >= dismissThreshold) {
                                                dismissDragYAnim.animateTo(
                                                    targetValue = if (dismissDragYAnim.value > 0f) viewportHeight else -viewportHeight,
                                                    animationSpec = tween(durationMillis = 150)
                                                )
                                                onDismiss()
                                            } else {
                                                animateViewerState(
                                                    scale = scaleAnim,
                                                    offsetX = offsetXAnim,
                                                    offsetY = offsetYAnim,
                                                    dismissDragY = dismissDragYAnim,
                                                    pagerDragX = pagerDragXAnim,
                                                    targetScale = 1f,
                                                    targetOffsetX = 0f,
                                                    targetOffsetY = 0f,
                                                    targetDismissY = 0f,
                                                    targetPagerX = 0f
                                                )
                                            }
                                        }

                                        ViewerDragMode.None -> {
                                            animateViewerState(
                                                scale = scaleAnim,
                                                offsetX = offsetXAnim,
                                                offsetY = offsetYAnim,
                                                dismissDragY = dismissDragYAnim,
                                                pagerDragX = pagerDragXAnim,
                                                targetScale = 1f,
                                                targetOffsetX = 0f,
                                                targetOffsetY = 0f,
                                                targetDismissY = 0f,
                                                targetPagerX = 0f
                                            )
                                        }
                                    }
                                    dragMode = ViewerDragMode.None
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isZoomed) {
                    if (selectedBitmap != null) {
                        ViewerPageImage(
                            bitmap = selectedBitmap,
                            contentDescription = selectedImage?.fileName ?: "image",
                            translationX = activeTranslationX,
                            translationY = activeTranslationY,
                            scale = scale * dismissScale,
                            alpha = imageAlpha
                        )
                    } else {
                        UIKit.Loader(size = 30)
                    }
                } else {
                    previousImage?.let { image ->
                        ViewerPageImage(
                            bitmap = previousBitmap,
                            contentDescription = image.fileName ?: "image",
                            translationX = pagerDragX - viewportWidth,
                            translationY = dismissDragY,
                            scale = dismissScale,
                            alpha = imageAlpha
                        )
                    }
                    nextImage?.let { image ->
                        ViewerPageImage(
                            bitmap = nextBitmap,
                            contentDescription = image.fileName ?: "image",
                            translationX = pagerDragX + viewportWidth,
                            translationY = dismissDragY,
                            scale = dismissScale,
                            alpha = imageAlpha
                        )
                    }
                    if (selectedBitmap != null) {
                        ViewerPageImage(
                            bitmap = selectedBitmap,
                            contentDescription = selectedImage?.fileName ?: "image",
                            translationX = pagerDragX,
                            translationY = dismissDragY,
                            scale = dismissScale,
                            alpha = imageAlpha
                        )
                    } else {
                        UIKit.Loader(size = 30)
                    }
                }
            }

            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 6.dp)
            ) {
                MediaOverlayHeader(
                    title = selectedImage?.fileName?.takeIf { it.isNotBlank() } ?: "Без названия",
                    onDismiss = onDismiss,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            if (images.size > 1) {
                AnimatedVisibility(
                    visible = chromeVisible && scale <= 1.05f,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x80181818))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 7.dp, vertical = 7.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}
                    ) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            images.forEachIndexed { index, image ->
                                val thumb by rememberViewerBitmap(
                                    asset = image.asset,
                                    loadImageBytes = loadImageBytes,
                                    bitmapCache = bitmapCache
                                )
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .border(
                                            width = if (index == selectedIndex) 1.dp else 0.dp,
                                            color = Color.White.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedIndex = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (thumb != null) {
                                        Image(
                                            bitmap = thumb!!,
                                            contentDescription = image.fileName ?: "thumb",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerPageImage(
    bitmap: ImageBitmap?,
    contentDescription: String,
    translationX: Float,
    translationY: Float,
    scale: Float,
    alpha: Float
) {
    if (bitmap == null) return

    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = translationX,
                translationY = translationY,
                alpha = alpha
            ),
        contentScale = ContentScale.Fit
    )
}

private suspend fun animateViewerPageSwitch(
    direction: Int,
    viewportWidth: Float,
    scale: Animatable<Float, AnimationVector1D>,
    offsetX: Animatable<Float, AnimationVector1D>,
    offsetY: Animatable<Float, AnimationVector1D>,
    dismissDragY: Animatable<Float, AnimationVector1D>,
    pagerDragX: Animatable<Float, AnimationVector1D>,
    onIndexChanged: () -> Unit
) {
    val exitTargetX = if (direction > 0) -viewportWidth else viewportWidth

    pagerDragX.animateTo(
        targetValue = exitTargetX,
        animationSpec = tween(durationMillis = 220)
    )
    scale.snapTo(1f)
    offsetX.snapTo(0f)
    offsetY.snapTo(0f)
    dismissDragY.snapTo(0f)
    onIndexChanged()
    pagerDragX.snapTo(0f)
}

private suspend fun animateViewerState(
    scale: Animatable<Float, AnimationVector1D>,
    offsetX: Animatable<Float, AnimationVector1D>,
    offsetY: Animatable<Float, AnimationVector1D>,
    dismissDragY: Animatable<Float, AnimationVector1D>,
    pagerDragX: Animatable<Float, AnimationVector1D>,
    targetScale: Float,
    targetOffsetX: Float,
    targetOffsetY: Float,
    targetDismissY: Float,
    targetPagerX: Float
) {
    val imageSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    coroutineScope {
        launch { scale.animateTo(targetScale, animationSpec = imageSpring) }
        launch { offsetX.animateTo(targetOffsetX, animationSpec = imageSpring) }
        launch { offsetY.animateTo(targetOffsetY, animationSpec = imageSpring) }
        launch { dismissDragY.animateTo(targetDismissY, animationSpec = imageSpring) }
        launch { pagerDragX.animateTo(targetPagerX, animationSpec = imageSpring) }
    }
}

private suspend fun PointerInputScope.detectTransformGesturesWithEnd(
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, pointerCount: Int) -> Unit,
    onGestureEnd: () -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)

        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        while (true) {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (canceled) break

            val zoomChange = event.calculateZoom()
            val panChange = event.calculatePan()
            val pointerCount = event.changes.count { it.pressed }

            if (!pastTouchSlop) {
                zoom *= zoomChange
                pan += panChange

                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                val zoomMotion = abs(1 - zoom) * centroidSize
                val panMotion = pan.getDistance()
                if (zoomMotion > touchSlop || panMotion > touchSlop) {
                    pastTouchSlop = true
                }
            }

            if (pastTouchSlop) {
                val centroid = event.calculateCentroid(useCurrent = false)
                if (zoomChange != 1f || panChange != Offset.Zero) {
                    onGesture(centroid, panChange, zoomChange, pointerCount)
                }
                event.changes.forEach { change ->
                    if (change.positionChanged()) {
                        change.consume()
                    }
                }
            }
            if (!event.changes.any { it.pressed }) break
        }

        onGestureEnd()
    }
}

private fun viewerMaxOffsets(
    containerWidth: Float,
    containerHeight: Float,
    bitmap: ImageBitmap?,
    scale: Float
): Pair<Float, Float> {
    if (containerWidth <= 1f || containerHeight <= 1f || bitmap == null) return 0f to 0f
    if (scale <= 1f) return 0f to 0f

    val imageWidth = bitmap.width.toFloat().coerceAtLeast(1f)
    val imageHeight = bitmap.height.toFloat().coerceAtLeast(1f)
    val imageAspect = imageWidth / imageHeight
    val containerAspect = containerWidth / containerHeight

    val baseWidth: Float
    val baseHeight: Float
    if (imageAspect > containerAspect) {
        baseWidth = containerWidth
        baseHeight = containerWidth / imageAspect
    } else {
        baseHeight = containerHeight
        baseWidth = containerHeight * imageAspect
    }

    val scaledWidth = baseWidth * scale
    val scaledHeight = baseHeight * scale

    val maxX = max(0f, (scaledWidth - containerWidth) / 2f)
    val maxY = max(0f, (scaledHeight - containerHeight) / 2f)
    return maxX to maxY
}

@Composable
private fun rememberViewerBitmap(
    asset: PostImageAsset?,
    loadImageBytes: suspend (PostImageAsset) -> ByteArray?,
    bitmapCache: MutableMap<String, ImageBitmap>
) = key(asset?.cacheKey) {
    produceState<ImageBitmap?>(initialValue = asset?.cacheKey?.let(bitmapCache::get)) {
        val source = asset ?: return@produceState
        bitmapCache[source.cacheKey]?.let { cached ->
            value = cached
            return@produceState
        }
        val bytes = runCatching { loadImageBytes(source) }.getOrNull() ?: return@produceState
        if (bytes.isEmpty()) return@produceState
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val image = bitmap?.asImageBitmap() ?: return@produceState
        bitmapCache[source.cacheKey] = image
        value = image
    }
}
