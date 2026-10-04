package elemsocial.com.ui.pack.components.posts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.sp
import elemsocial.com.R
import elemsocial.com.domain.model.PostReactions
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.components.modals.ElementContextMenuItem
import elemsocial.com.ui.pack.components.text.ElementLinkText
import elemsocial.com.ui.pack.theme.ElementUiPalette

private const val PostPreviewLength = 700
private const val PostMaxPreviewLength = 4000
private const val PostCollapsedMaxHeightDp = 400
private const val PostExpandedToggleSpaceDp = 30
private const val PostCollapsedToggleFadeDp = 72

@Composable
fun ElementPostCard(
    postId: Int,
    authorName: String,
    authorUsername: String,
    authorAvatar: (@Composable () -> Unit)? = null,
    authorBadge: (@Composable () -> Unit)? = null,
    onAuthorClick: (() -> Unit)? = null,
    dateText: String,
    text: String?,
    archived: Boolean,
    deleted: Boolean,
    edited: Boolean,
    likes: Int,
    dislikes: Int,
    comments: Int,
    liked: Boolean,
    disliked: Boolean,
    interactionsEnabled: Boolean,
    showCommentButton: Boolean = true,
    shareLink: String,
    governItems: List<ElementContextMenuItem>,
    likeBurstTrigger: Int,
    likeBurstOrigin: Offset?,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onComment: () -> Unit,
    onCopyLink: () -> Unit,
    onDoubleTapLike: (Offset) -> Unit,
    reactions: PostReactions? = null,
    onReactionToggle: ((reaction: String, isCurrentlySet: Boolean) -> Unit)? = null,
    showShadow: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var shareOpen by remember(postId) { mutableStateOf(false) }
    var governOpen by remember(postId) { mutableStateOf(false) }
    var governAnchor by remember(postId) { mutableStateOf<IntRect?>(null) }

    UIKit.Block(
        modifier = modifier,
        showShadow = showShadow,
        contentPadding = 6.5.dp
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(postId, interactionsEnabled, onDoubleTapLike) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (interactionsEnabled) {
                                        onDoubleTapLike(offset)
                                    }
                                }
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        TopBar(
                            authorName = authorName,
                            authorUsername = authorUsername,
                            authorAvatar = authorAvatar,
                            authorBadge = authorBadge,
                            onAuthorClick = onAuthorClick,
                            dateText = dateText,
                            archived = archived,
                            deleted = deleted,
                            showGovernButton = governItems.isNotEmpty(),
                            onOpenGovern = {
                                shareOpen = false
                                governOpen = true
                            },
                            onGovernAnchorChanged = { governAnchor = it }
                        )

                        if (!text.isNullOrEmpty()) {
                            PostTextBlock(
                                postId = postId,
                                text = text,
                                onDoubleTapLike = {
                                    if (interactionsEnabled) {
                                        onDoubleTapLike(it)
                                    }
                                }
                            )
                        }

                        content()
                    }

                    UIKit.LikeBurst(
                        trigger = likeBurstTrigger,
                        origin = likeBurstOrigin,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                InteractionContainer(
                    reactions = reactions,
                    comments = comments,
                    edited = edited,
                    interactionsEnabled = interactionsEnabled,
                    showCommentButton = showCommentButton,
                    shareOpen = shareOpen,
                    shareLink = shareLink,
                    onReactionToggle = onReactionToggle,
                    onComment = onComment,
                    onCopyLink = onCopyLink,
                    onOpenShare = { shareOpen = true },
                    onCloseShare = { shareOpen = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            UIKit.ContextMenu(
                expanded = governOpen && governItems.isNotEmpty(),
                anchorBounds = governAnchor,
                items = governItems,
                onDismissRequest = { governOpen = false }
            )
        }
    }
}

@Composable
private fun PostTextBlock(
    postId: Int,
    text: String,
    onDoubleTapLike: (Offset) -> Unit
) {
    var expanded by remember(postId, text) { mutableStateOf(false) }

    val fullTextPhysical = remember(text) {
        if (text.length <= PostMaxPreviewLength) {
            text
        } else {
            val idx = text.indexOf(' ', PostMaxPreviewLength)
            if (idx == -1) {
                text.take(PostMaxPreviewLength) + "..."
            } else {
                text.substring(0, idx) + "..."
            }
        }
    }

    val textToRender = if (expanded) text else fullTextPhysical
    val showToggle = text.length > PostPreviewLength
    val shouldHideCss = !expanded && text.length > PostPreviewLength
    val bottomPadding = if (showToggle && expanded) PostExpandedToggleSpaceDp.dp else 0.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding)
        ) {
            Box(
                modifier = if (shouldHideCss) {
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = PostCollapsedMaxHeightDp.dp)
                        .clipToBounds()
                } else {
                    Modifier.fillMaxWidth()
                }
            ) {
                ElementLinkText(
                    text = textToRender,
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = ElementUiPalette.TextPrimary,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onDoubleTap = onDoubleTapLike
                )
            }
        }

        if (showToggle) {
            PostTextToggleLayer(
                expanded = expanded,
                onToggle = { expanded = !expanded },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PostTextToggleLayer(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fadeHeight = if (expanded) PostExpandedToggleSpaceDp.dp else PostCollapsedToggleFadeDp.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(fadeHeight)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        ElementUiPalette.Block.copy(alpha = if (expanded) 0.88f else 0.72f),
                        ElementUiPalette.Block
                    )
                )
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PostExpandedToggleSpaceDp.dp)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (expanded) "Свернуть" else "Полный текст",
                color = ElementUiPalette.TextSecondary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TopBar(
    authorName: String,
    authorUsername: String,
    authorAvatar: (@Composable () -> Unit)?,
    authorBadge: (@Composable () -> Unit)?,
    onAuthorClick: (() -> Unit)?,
    dateText: String,
    archived: Boolean,
    deleted: Boolean,
    showGovernButton: Boolean,
    onOpenGovern: () -> Unit,
    onGovernAnchorChanged: (IntRect) -> Unit
) {
    val avatarModifier = if (onAuthorClick != null) {
        Modifier.clickable(onClick = onAuthorClick)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (authorAvatar != null) {
            Box(modifier = avatarModifier) {
                authorAvatar()
            }
        } else {
            Box(
                modifier = Modifier
                    .then(avatarModifier)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF7F6EB0), Color(0xFFA19DB1)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = authorName.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
                .let { base ->
                    if (onAuthorClick != null) {
                        base.clickable(onClick = onAuthorClick)
                    } else {
                        base
                    }
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = authorName,
                    color = ElementUiPalette.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (authorBadge != null) {
                    Box(modifier = Modifier.padding(start = 3.dp)) {
                        authorBadge()
                    }
                }
            }
            Text(
                text = buildString {
                    append("@")
                    append(authorUsername)
                    if (dateText.isNotBlank()) {
                        append(" • ")
                        append(dateText)
                    }
                },
                style = TextStyle(
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    ),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (archived || deleted) {
            Row(
                modifier = Modifier.padding(end = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (archived) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = ElementUiPalette.InteractionText,
                        modifier = Modifier.size(15.dp)
                    )
                }
                if (deleted) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = ElementUiPalette.InteractionText,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        if (showGovernButton) {
            Box {
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { coordinates ->
                            val rect = coordinates.boundsInWindow()
                            onGovernAnchorChanged(
                                IntRect(
                                    left = rect.left.toInt(),
                                    top = rect.top.toInt(),
                                    right = rect.right.toInt(),
                                    bottom = rect.bottom.toInt()
                                )
                            )
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElementUiPalette.Block)
                        .clickable(onClick = onOpenGovern)
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_element_dots),
                        contentDescription = null,
                        tint = ElementUiPalette.InteractionText,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractionContainer(
    reactions: PostReactions?,
    comments: Int,
    edited: Boolean,
    interactionsEnabled: Boolean,
    showCommentButton: Boolean,
    shareOpen: Boolean,
    shareLink: String,
    onReactionToggle: ((reaction: String, isCurrentlySet: Boolean) -> Unit)?,
    onComment: () -> Unit,
    onCopyLink: () -> Unit,
    onOpenShare: () -> Unit,
    onCloseShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionsScale by animateFloatAsState(
        targetValue = if (shareOpen) 0.5f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "postShareScale"
    )
    val interactionsAlpha by animateFloatAsState(
        targetValue = if (shareOpen) 0f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "postShareAlpha"
    )

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = interactionsScale
                    scaleY = interactionsScale
                    alpha = interactionsAlpha
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onReactionToggle != null) {
                    ElementReactionBar(
                        reactions = reactions ?: PostReactions(),
                        onToggle = onReactionToggle
                    )
                }

                if (showCommentButton) {
                    PostInteractionButton(
                        icon = painterResource(id = R.drawable.ic_element_comment),
                        active = false,
                        enabled = true,
                        count = comments,
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier.padding(start = 7.dp),
                        onClick = {
                            onCloseShare()
                            onComment()
                        }
                    )
                }

                PostInteractionButton(
                    icon = painterResource(id = R.drawable.ic_element_share),
                    active = false,
                    enabled = true,
                    text = "Поделиться",
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.padding(start = 7.dp),
                    onClick = onOpenShare
                )

                if (edited) {
                    Spacer(modifier = Modifier.width(0.dp).weight(1f))
                    Text(
                        text = "изменено",
                        color = ElementUiPalette.TextLite,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(start = 8.dp, end = 2.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = shareOpen,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(200)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(200)
            ) + fadeOut(animationSpec = tween(200)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(ElementUiPalette.Block)
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PostInteractionButton(
                        icon = painterResource(id = R.drawable.ic_element_back),
                        active = false,
                        enabled = true,
                        text = "Назад",
                        shape = RoundedCornerShape(30.dp),
                        onClick = onCloseShare
                    )

                    Box(
                        modifier = Modifier
                            .padding(start = 7.dp, end = 2.dp)
                            .clip(RoundedCornerShape(topStart = 100.dp, bottomStart = 100.dp))
                            .background(ElementUiPalette.Interaction)
                            .height(30.dp)
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        SelectionContainer {
                            BasicText(
                                text = shareLink,
                                maxLines = 1,
                                style = TextStyle(
                                    color = ElementUiPalette.InteractionText,
                                    fontSize = 13.sp
                                ),
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(topEnd = 100.dp, bottomEnd = 100.dp))
                            .background(ElementUiPalette.Interaction)
                            .clickable(onClick = onCopyLink)
                            .height(30.dp)
                            .width(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_element_copy),
                            contentDescription = null,
                            tint = ElementUiPalette.InteractionText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostInteractionButton(
    icon: Painter,
    active: Boolean,
    enabled: Boolean,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    count: Int? = null,
    text: String? = null,
    onClick: () -> Unit
) {
    val tint = if (active) ElementUiPalette.Accent else ElementUiPalette.InteractionText
    val background = if (active) ElementUiPalette.Accent.copy(alpha = 0.20f) else ElementUiPalette.Interaction

    Row(
        modifier = modifier
            .clip(shape)
            .background(background)
            .height(30.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(23.dp)
        )

        when {
            !text.isNullOrBlank() -> {
                Text(
                    text = text,
                    color = tint,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 5.dp),
                    maxLines = 1
                )
            }

            (count ?: 0) > 0 -> {
                Text(
                    text = count.toString(),
                    color = tint,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 5.dp),
                    maxLines = 1
                )
            }
        }
    }
}
