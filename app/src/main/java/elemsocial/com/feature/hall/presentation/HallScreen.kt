package elemsocial.com.feature.hall.presentation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.R
import elemsocial.com.domain.model.HallUser
import elemsocial.com.core.plugins.ElementPluginRuntime
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.feature.home.presentation.HomeGateway
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val VERIFY_ICON_ID = "VERIFY"
private const val GOLD_ICON_ID = "GOLD"

@Composable
fun HallScreen(
    hallGateway: HallGateway,
    homeGateway: HomeGateway,
    onOpenProfile: (String) -> Unit,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 90.dp,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var loading by remember { mutableStateOf(false) }
    var users by remember { mutableStateOf<List<HallUser>>(emptyList()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun loadHall() {
        if (loading) return
        loading = true
        scope.launch {
            val result = runCatching { hallGateway.loadHall(0) }.getOrNull()
            if (result?.isSuccess == true) {
                users = result.users
                errorText = null
            } else {
                errorText = result?.message ?: "Не удалось загрузить зал славы"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadHall()
    }

    val visualHall = ElementPluginRuntime.visualHall(context)
    val shownUsers = if (visualHall != null) {
        val fakeUser = HallUser(
            id = -999999,
            name = visualHall.displayName,
            username = visualHall.username,
            eballs = visualHall.balance
        )
        listOf(fakeUser) + users.filterNot { it.username.equals(fakeUser.username, ignoreCase = true) }
    } else {
        users
    }

    val first = shownUsers.getOrNull(0)
    val second = shownUsers.getOrNull(1)
    val third = shownUsers.getOrNull(2)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ElementUiPalette.Body),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = topPadding,
            bottom = bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            UIKit.Block {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Зал славы",
                        color = ElementUiPalette.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(92.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        PodiumPlace(
                            user = second,
                            fallback = "2 место",
                            height = 0.8f,
                            gradient = Brush.verticalGradient(
                                listOf(Color(0xFFB26FB5), Color(0xFF3F3462))
                            ),
                            onOpenProfile = onOpenProfile,
                            homeGateway = homeGateway,
                            modifier = Modifier.weight(1f)
                        )
                        PodiumPlace(
                            user = first,
                            fallback = "1 место",
                            height = 1f,
                            gradient = Brush.verticalGradient(
                                listOf(Color(0xFFAC6CD8), Color(0xFF282949))
                            ),
                            onOpenProfile = onOpenProfile,
                            homeGateway = homeGateway,
                            modifier = Modifier.weight(1f)
                        )
                        PodiumPlace(
                            user = third,
                            fallback = "3 место",
                            height = 0.6f,
                            gradient = Brush.verticalGradient(
                                listOf(Color(0xFF71D0F9), Color(0xFF3F3462))
                            ),
                            onOpenProfile = onOpenProfile,
                            homeGateway = homeGateway,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (!errorText.isNullOrBlank()) {
            item {
                UIKit.Block {
                    Text(
                        text = errorText.orEmpty(),
                        color = ElementUiPalette.Error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        if (loading && users.isEmpty()) {
            items(6) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElementUiPalette.Block)
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    UIKit.SkeletonHallRow(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        itemsIndexed(
            items = shownUsers.drop(3),
            key = { index, user -> "${user.id}:${user.username}:$index" }
        ) { _, user ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElementUiPalette.Block)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    ElementUiPalette.Block,
                                    ElementUiPalette.Block,
                                    ElementUiPalette.Accent.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HallAvatar(
                        name = user.name,
                        avatar = user.avatar,
                        homeGateway = homeGateway,
                        size = 40.dp,
                        modifier = Modifier.clickable { onOpenProfile(user.username) }
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = user.name,
                                color = ElementUiPalette.TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { onOpenProfile(user.username) }
                            )
                            HallUserBadges(icons = user.icons, size = 16.dp)
                        }
                        Text(
                            text = "@${user.username}",
                            color = ElementUiPalette.TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatHallEballs(user.eballs),
                            color = ElementUiPalette.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        UIKit.Eball(size = 30.dp, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumPlace(
    user: HallUser?,
    fallback: String,
    height: Float,
    gradient: Brush,
    onOpenProfile: (String) -> Unit,
    homeGateway: HomeGateway,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxSize()
            .padding(top = ((1f - height) * 100).dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(gradient)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-56).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HallAvatar(
                name = user?.name ?: fallback,
                avatar = user?.avatar,
                homeGateway = homeGateway,
                size = 86.dp,
                modifier = if (user?.username.isNullOrBlank()) Modifier else Modifier.clickable {
                    user?.username?.let(onOpenProfile)
                }
            )
            Text(
                text = user?.name ?: fallback,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.padding(bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            UIKit.Eball(size = 30.dp, fontSize = 15.sp)
            Text(
                text = formatHallEballs(user?.eballs ?: 0.0),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HallAvatar(
    name: String,
    avatar: PostImageAsset?,
    homeGateway: HomeGateway,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val bitmap by rememberHallImageBitmap(avatar, homeGateway)
    val shape = CircleShape
    val firstLetter = name.firstOrNull()?.uppercase() ?: "U"

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(ElementUiPalette.BlockSoft),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = firstLetter,
                color = ElementUiPalette.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}

@Composable
private fun rememberHallImageBitmap(
    asset: PostImageAsset?,
    homeGateway: HomeGateway
): androidx.compose.runtime.State<ImageBitmap?> {
    return produceState<ImageBitmap?>(initialValue = null, key1 = asset) {
        value = null
        if (asset == null) return@produceState
        val bytes = runCatching { homeGateway.loadImageBytes(asset) }.getOrNull() ?: return@produceState
        value = runCatching {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
}

private fun List<String>?.hasVerifyBadge(): Boolean {
    return this?.any { it.equals(VERIFY_ICON_ID, ignoreCase = true) } == true
}

private fun List<String>?.hasGoldBadge(): Boolean {
    return this?.any { it.equals(GOLD_ICON_ID, ignoreCase = true) } == true
}

@Composable
private fun HallUserBadges(
    icons: List<String>?,
    size: androidx.compose.ui.unit.Dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icons.hasVerifyBadge()) {
            Image(
                painter = painterResource(id = R.drawable.ic_profile_verify),
                contentDescription = null,
                modifier = Modifier.size(size)
            )
        }
        if (icons.hasGoldBadge()) {
            Image(
                painter = painterResource(id = R.drawable.ic_profile_gold),
                contentDescription = null,
                modifier = Modifier.size(size)
            )
        }
    }
}

private fun formatHallEballs(value: Double): String {
    val roundedInt = value.toLong()
    return if (abs(value - roundedInt.toDouble()) < 0.000_001) {
        roundedInt.toString()
    } else {
        value.toString().trimEnd('0').trimEnd('.')
    }
}
