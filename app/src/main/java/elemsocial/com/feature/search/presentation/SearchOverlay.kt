package elemsocial.com.feature.search.presentation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.core.model.LocalTransparencyMode
import elemsocial.com.core.model.TransparencyMode
import elemsocial.com.core.model.glassAlphaFor
import elemsocial.com.core.model.glassSoftAlphaFor
import elemsocial.com.core.model.shouldUseGlassBlur
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.SearchCategory
import elemsocial.com.domain.model.SearchResultItem
import elemsocial.com.feature.home.presentation.HomeGateway
import elemsocial.com.feature.music.presentation.MusicCompactMetaBlock
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.delay

@Composable
fun SearchOverlay(
    query: String,
    searchGateway: SearchGateway,
    homeGateway: HomeGateway,
    onOpenProfile: (String) -> Unit,
    onOpenPost: (Int) -> Unit,
    onOpenMusicTrack: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    transparencyMode: TransparencyMode = TransparencyMode.ADAPTIVE
) {
    var category by remember { mutableStateOf(SearchCategory.Users) }
    var loading by remember(query, category) { mutableStateOf(false) }
    var errorText by remember(query, category) { mutableStateOf<String?>(null) }
    var results by remember(query, category) { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    val panelHeight = LocalConfiguration.current.screenHeightDp.dp * 0.70f
    val panelAlpha = glassAlphaFor(transparencyMode)
    val panelSoftAlpha = glassSoftAlphaFor(transparencyMode)
    val panelColor = ElementUiPalette.Block.copy(alpha = panelAlpha)
    val panelSoftColor = ElementUiPalette.BlockSoft.copy(alpha = panelSoftAlpha)
    val blurEnabled = shouldUseGlassBlur(transparencyMode)
    val panelShape = RoundedCornerShape(30.dp)

    LaunchedEffect(query, category) {
        val value = query.trim()
        if (value.length < 2) {
            results = emptyList()
            errorText = null
            loading = false
            return@LaunchedEffect
        }

        loading = true
        delay(160)
        val result = runCatching {
            searchGateway.search(category, value)
        }.getOrNull()
        if (result?.isSuccess == true) {
            results = result.results.filter {
                when (category) {
                    SearchCategory.Users -> it is SearchResultItem.User
                    SearchCategory.Posts -> it is SearchResultItem.Post
                    SearchCategory.Music -> it is SearchResultItem.Music
                }
            }
            errorText = null
        } else {
            results = emptyList()
            errorText = result?.message ?: "Не удалось выполнить поиск"
        }
        loading = false
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .height(panelHeight)
                .shadow(
                    elevation = 18.dp,
                    shape = panelShape,
                    ambientColor = ElementUiPalette.TextPrimary.copy(alpha = 0.12f),
                    spotColor = ElementUiPalette.TextPrimary.copy(alpha = 0.16f)
                )
                .clip(panelShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(panelColor, panelSoftColor)
                    )
                )
                .border(1.dp, ElementUiPalette.scaledBorder(0.9f), panelShape)
        ) {
            if (blurEnabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(26.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(panelColor, panelSoftColor)
                            )
                        )
                )
            }

            SearchCategoryTabs(
                selected = category,
                onSelect = { category = it },
                modifier = Modifier.align(Alignment.TopStart)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 38.dp)
            ) {
                when {
                    query.trim().length < 2 -> {
                        SearchMessage(text = "Введите минимум 2 символа")
                    }

                    loading -> {
                        SearchMessage(text = "Загрузка...")
                    }

                    !errorText.isNullOrBlank() -> {
                        SearchMessage(
                            text = errorText.orEmpty(),
                            color = ElementUiPalette.Error
                        )
                    }

                    results.isEmpty() -> {
                        SearchMessage(text = "Ничего не найдено")
                    }

                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                contentPadding = PaddingValues(top = 11.dp, bottom = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                items(results) { item ->
                                    when (item) {
                                        is SearchResultItem.User -> SearchUserRow(
                                            item = item,
                                            homeGateway = homeGateway,
                                            onClick = { onOpenProfile(item.username) }
                                        )

                                        is SearchResultItem.Post -> SearchPostRow(
                                            item = item,
                                            homeGateway = homeGateway,
                                            onClick = { onOpenPost(item.id) }
                                        )

                                        is SearchResultItem.Music -> SearchMusicRow(
                                            item = item,
                                            homeGateway = homeGateway,
                                            onClick = { onOpenMusicTrack(item.id) }
                                        )
                                    }
                                }
                            }

                            SearchViewportScrims(
                                color = panelColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchCategoryTabs(
    selected: SearchCategory,
    onSelect: (SearchCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChipColor = ElementUiPalette.BlockSoft.copy(
        alpha = glassSoftAlphaFor(LocalTransparencyMode.current)
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchCategory.entries.forEach { item ->
                val active = item == selected
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .widthIn(min = 84.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) activeChipColor else Color.Transparent)
                        .clickable { onSelect(item) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.webTitle(),
                        color = if (active) ElementUiPalette.TextPrimary else ElementUiPalette.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchViewportScrims(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(18.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(22.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0f), color)
                    )
                )
        )
    }
}

@Composable
private fun SearchMessage(
    text: String,
    color: Color = ElementUiPalette.TextPrimary
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SearchUserRow(
    item: SearchResultItem.User,
    homeGateway: HomeGateway,
    onClick: () -> Unit
) {
    val rowColor = ElementUiPalette.BlockSoft.copy(
        alpha = glassSoftAlphaFor(LocalTransparencyMode.current)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TinyAvatar(
            name = item.name,
            asset = item.avatar,
            homeGateway = homeGateway,
            modifier = Modifier.size(40.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = ElementUiPalette.TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.subscribers} подписчиков • ${item.posts} постов",
                color = ElementUiPalette.TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchPostRow(
    item: SearchResultItem.Post,
    homeGateway: HomeGateway,
    onClick: () -> Unit
) {
    val rowColor = ElementUiPalette.BlockSoft.copy(
        alpha = glassSoftAlphaFor(LocalTransparencyMode.current)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TinyAvatar(
            name = item.authorName,
            asset = item.authorAvatar,
            homeGateway = homeGateway,
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(
                topStart = 100.dp,
                topEnd = 100.dp,
                bottomEnd = 10.dp,
                bottomStart = 100.dp
            )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.authorName,
                color = ElementUiPalette.TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.text?.takeIf { it.isNotBlank() } ?: "Пост #${item.id}",
                color = ElementUiPalette.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TinyAvatar(
    name: String,
    asset: PostImageAsset?,
    homeGateway: HomeGateway,
    modifier: Modifier = Modifier.size(40.dp),
    shape: Shape = CircleShape
) {
    val bitmap by rememberImageBitmap(
        asset = asset,
        homeGateway = homeGateway
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF7F6EB0), Color(0xFFA19DB1)))),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

private fun SearchCategory.webTitle(): String {
    return when (this) {
        SearchCategory.Users -> "пользователи"
        SearchCategory.Posts -> "посты"
        SearchCategory.Music -> "музыка"
    }
}

@Composable
private fun SearchMusicRow(
    item: SearchResultItem.Music,
    homeGateway: HomeGateway,
    onClick: () -> Unit
) {
    val rowColor = ElementUiPalette.BlockSoft.copy(
        alpha = glassSoftAlphaFor(LocalTransparencyMode.current)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TinyAvatar(
            name = item.title,
            asset = item.cover,
            homeGateway = homeGateway,
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(6.dp)
        )
        MusicCompactMetaBlock(
            title = item.title,
            artist = item.artist,
            modifier = Modifier.weight(1f),
            titleFontSize = 17.sp,
            artistFontSize = 14.sp,
            titleLineHeight = 17.sp,
            artistLineHeight = 14.sp,
            titleFontWeight = FontWeight.Medium
        )
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = ElementUiPalette.TextSecondary
        )
    }
}

@Composable
private fun rememberImageBitmap(
    asset: PostImageAsset?,
    homeGateway: HomeGateway
) = produceState<ImageBitmap?>(initialValue = null, key1 = asset?.cacheKey) {
    val target = asset ?: return@produceState
    val bytes = runCatching { homeGateway.loadImageBytes(target) }.getOrNull() ?: return@produceState
    if (bytes.isEmpty()) return@produceState

    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    value = bitmap?.asImageBitmap()
}
