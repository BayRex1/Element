package elemsocial.com.feature.wallet.presentation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import elemsocial.com.R
import elemsocial.com.core.time.formatTimeAge
import elemsocial.com.core.plugins.ElementPluginRuntime
import elemsocial.com.domain.model.AuthGoldHistory
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.WalletReferralDashboard
import elemsocial.com.domain.model.WalletReferralHistoryItem
import elemsocial.com.domain.model.WalletTransaction
import elemsocial.com.domain.model.WalletUser
import elemsocial.com.feature.home.presentation.HomeGateway
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.components.buttons.ElementButtonVariant
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private const val WALLET_PAGE_SIZE = 25

@Composable
fun WalletScreen(
    walletGateway: WalletGateway,
    homeGateway: HomeGateway,
    initialBalance: Double?,
    goldStatus: Boolean,
    goldHistory: List<AuthGoldHistory>,
    onBalanceChanged: (Double) -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val visualBalance = ElementPluginRuntime.visualBalance(context, initialBalance ?: 0.0)
    var balance by remember(initialBalance, visualBalance) { mutableStateOf(visualBalance) }
    var activeTab by remember { mutableStateOf(0) }
    var transferOpen by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<WalletTransaction>>(emptyList()) }
    var loadingHistory by remember { mutableStateOf(false) }
    var historyStartIndex by remember { mutableStateOf(0) }
    var historyHasMore by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var subscriptionStatus by remember { mutableStateOf(goldStatus) }
    var subscriptionHistory by remember { mutableStateOf(goldHistory) }
    var goldActionLoading by remember { mutableStateOf(false) }
    var referralDashboard by remember { mutableStateOf<WalletReferralDashboard?>(null) }
    var referralHistory by remember { mutableStateOf<List<WalletReferralHistoryItem>>(emptyList()) }
    var referralLoading by remember { mutableStateOf(false) }
    var referralLoadingMore by remember { mutableStateOf(false) }
    var referralHasMore by remember { mutableStateOf(true) }
    var referralStartIndex by remember { mutableStateOf(0) }

    LaunchedEffect(initialBalance, visualBalance) {
        balance = visualBalance
    }

    LaunchedEffect(goldStatus) {
        subscriptionStatus = goldStatus
    }

    LaunchedEffect(goldHistory) {
        subscriptionHistory = goldHistory
    }

    fun loadHistory(reset: Boolean) {
        if (loadingHistory) return
        if (!reset && !historyHasMore) return
        val startIndex = if (reset) 0 else historyStartIndex
        loadingHistory = true
        scope.launch {
            val result = runCatching { walletGateway.loadHistory(startIndex) }.getOrNull()
            if (result?.isSuccess == true) {
                history = if (reset) {
                    result.transactions
                } else {
                    (history + result.transactions).distinctBy { it.id }
                }
                historyStartIndex = if (reset) {
                    result.transactions.size
                } else {
                    historyStartIndex + result.transactions.size
                }
                historyHasMore = result.transactions.size >= WALLET_PAGE_SIZE
                errorText = null
            } else {
                errorText = result?.message ?: "Не удалось загрузить историю"
            }
            loadingHistory = false
        }
    }

    fun loadReferralDashboard() {
        referralLoading = true
        scope.launch {
            val result = runCatching { walletGateway.loadReferralDashboard() }.getOrNull()
            if (result?.isSuccess == true) {
                referralDashboard = result.dashboard
            } else if (!result?.message.isNullOrBlank()) {
                errorText = result?.message
            }
            referralLoading = false
        }
    }

    fun loadReferralHistory(reset: Boolean) {
        if (referralLoadingMore) return
        if (!reset && !referralHasMore) return
        val startIndex = if (reset) 0 else referralStartIndex
        referralLoadingMore = true
        scope.launch {
            val result = runCatching { walletGateway.loadReferralHistory(startIndex, 25) }.getOrNull()
            if (result?.isSuccess == true) {
                referralHistory = if (reset) result.history else (referralHistory + result.history)
                referralStartIndex = if (reset) result.history.size else referralStartIndex + result.history.size
                referralHasMore = result.history.size >= 25
            } else if (!result?.message.isNullOrBlank()) {
                errorText = result?.message
            }
            referralLoadingMore = false
        }
    }

    fun payGold() {
        if (goldActionLoading) return
        goldActionLoading = true
        scope.launch {
            val result = runCatching { walletGateway.goldPay() }.getOrNull()
            if (result?.isSuccess == true) {
                subscriptionStatus = true
                balance = max(0.0, balance - 0.1)
                onBalanceChanged(balance)
                errorText = null
                subscriptionHistory = listOf(
                    AuthGoldHistory(status = 1, date = OffsetDateTime.now().toString())
                ) + subscriptionHistory
            } else {
                errorText = result?.message ?: "Не удалось оплатить подписку"
            }
            goldActionLoading = false
        }
    }

    fun activateGold(codeRaw: String) {
        val code = codeRaw.trim()
        if (code.isBlank() || goldActionLoading) return
        goldActionLoading = true
        scope.launch {
            val result = runCatching { walletGateway.goldActivate(code) }.getOrNull()
            if (result?.isSuccess == true) {
                subscriptionStatus = true
                errorText = null
                subscriptionHistory = listOf(
                    AuthGoldHistory(status = 1, date = OffsetDateTime.now().toString())
                ) + subscriptionHistory
            } else {
                errorText = result?.message ?: "Не удалось активировать код"
            }
            goldActionLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadHistory(reset = true)
        loadReferralDashboard()
        loadReferralHistory(reset = true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ElementUiPalette.Body)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                WalletBalanceCard(
                    balance = balance,
                    onTransferClick = { transferOpen = true }
                )
            }

            item {
                WalletTabs(
                    selectedIndex = activeTab,
                    onSelect = { activeTab = it }
                )
            }

            when (activeTab) {
                0 -> {
                    if (!errorText.isNullOrBlank()) {
                        item { ErrorItem(errorText.orEmpty()) }
                    }

                    when {
                        loadingHistory && history.isEmpty() -> {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    repeat(5) {
                                        UIKit.SkeletonWalletTransaction(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }

                        history.isEmpty() -> {
                            item {
                                UIKit.Block {
                                    Text(
                                        text = "Операций пока нет",
                                        color = ElementUiPalette.TextSecondary,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                    )
                                }
                            }
                        }

                        else -> {
                            items(history, key = { it.id }) { tx ->
                                WalletTransactionItem(
                                    transaction = tx,
                                    homeGateway = homeGateway,
                                    onOpenProfile = onOpenProfile
                                )
                            }
                        }
                    }

                    if (historyHasMore && history.isNotEmpty()) {
                        item {
                            UIKit.Button(
                                title = if (loadingHistory) "Загрузка..." else "Показать еще",
                                onClick = { loadHistory(reset = false) },
                                enabled = !loadingHistory,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                1 -> {
                    if (!errorText.isNullOrBlank()) {
                        item { ErrorItem(errorText.orEmpty()) }
                    }
                    item {
                        WalletSubscriptionTab(
                            subscriptionStatus = subscriptionStatus,
                            history = subscriptionHistory,
                            actionLoading = goldActionLoading,
                            onPay = ::payGold,
                            onActivate = ::activateGold
                        )
                    }
                }

                2 -> {
                    item {
                        WalletEarnInfo()
                    }
                }

                else -> {
                    item {
                        WalletReferralTab(
                            loading = referralLoading,
                            dashboard = referralDashboard,
                            history = referralHistory,
                            hasMore = referralHasMore,
                            loadingMore = referralLoadingMore,
                            homeGateway = homeGateway,
                            onOpenProfile = onOpenProfile,
                            onLoadMore = { loadReferralHistory(reset = false) }
                        )
                    }
                }
            }
        }

        if (transferOpen) {
            WalletTransferModal(
                walletGateway = walletGateway,
                homeGateway = homeGateway,
                balance = balance,
                onBalanceChanged = { next ->
                    balance = max(0.0, next)
                    onBalanceChanged(balance)
                    loadHistory(reset = true)
                },
                onClose = { transferOpen = false }
            )
        }
    }
}

@Composable
private fun WalletTabs(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
            val tabs = listOf("История", "Подписка", "Заработок", "Рефералы")
            UIKit.SegmentTabs(
                tabs = tabs,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                modifier = Modifier.fillMaxWidth()
            )
}

@Composable
private fun WalletBalanceCard(
    balance: Double,
    onTransferClick: () -> Unit
) {
    UIKit.Block {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Текущий баланс",
                color = ElementUiPalette.TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                UIKit.Eball(size = 40.dp, fontSize = 20.sp)
                Text(
                    text = formatEballs(balance),
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                WalletActionButton(
                    title = "Перевести",
                    icon = Icons.Default.Send,
                    onClick = onTransferClick
                )
            }
        }
    }
}

@Composable
private fun WalletActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ElementUiPalette.TextPrimary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            color = ElementUiPalette.TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun WalletTransactionItem(
    transaction: WalletTransaction,
    homeGateway: HomeGateway,
    onOpenProfile: (String) -> Unit
) {
    val positive = transaction.incoming ||
        transaction.type == "referral_reward_inviter" ||
        transaction.type == "referral_reward_invited"
    val amountText = when {
        transaction.type == "referral_reward_inviter" || transaction.type == "referral_reward_invited" -> {
            "+${formatEballs(transaction.amount)}"
        }

        transaction.incoming -> {
            "+${formatEballs(transaction.amount)}"
        }

        else -> {
            "-${formatEballs(transaction.amount + transaction.fee)}"
        }
    }

    val title = when (transaction.type) {
        "gift_pay" -> if (transaction.incoming) "Подарок получен" else "Подарок отправлен"
        "referral_reward_inviter" -> "Реферальная награда"
        "referral_reward_invited" -> "Бонус за приглашение"
        else -> if (transaction.incoming) "Получено от" else "Отправлено пользователю"
    }

    val username = if (transaction.incoming) {
        transaction.sender?.username
    } else {
        transaction.recipient?.username
    }?.takeIf { it.isNotBlank() }

    UIKit.Block {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            WalletTxIcon(
                transaction = transaction,
                homeGateway = homeGateway
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (!username.isNullOrBlank() && transaction.type != "gift_pay") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = ElementUiPalette.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@$username",
                            color = ElementUiPalette.Accent,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenProfile(username) }
                        )
                    }
                } else {
                    Text(
                        text = title,
                        color = ElementUiPalette.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (transaction.type == "gift_pay" && !transaction.gift?.name.isNullOrBlank()) {
                    Text(
                        text = transaction.gift?.name.orEmpty(),
                        color = ElementUiPalette.TextPrimary.copy(alpha = 0.82f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = formatTimeAge(transaction.date),
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 12.sp
                )

                if (!transaction.message.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElementUiPalette.BlockSoft)
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = transaction.message.orEmpty(),
                            color = ElementUiPalette.TextPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = amountText,
                    color = if (positive) Color(0xFF52A952) else Color(0xFFE15757),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                UIKit.Eball(size = 22.dp, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun WalletTxIcon(
    transaction: WalletTransaction,
    homeGateway: HomeGateway
) {
    when (transaction.type) {
        "gift_pay" -> WalletGiftTransactionIcon(
            transaction = transaction,
            homeGateway = homeGateway
        )

        else -> WalletTransferTransactionIcon(
            transaction = transaction,
            homeGateway = homeGateway
        )
    }
}

@Composable
private fun WalletGiftTransactionIcon(
    transaction: WalletTransaction,
    homeGateway: HomeGateway
) {
    val giftBitmap by rememberWalletImageBitmap(transaction.gift?.image, homeGateway)
    val sender = transaction.sender
    val recipient = transaction.giftRecipient ?: transaction.recipient

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(ElementUiPalette.BlockSoft),
        contentAlignment = Alignment.Center
    ) {
        if (sender != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-2).dp, y = (-2).dp)
            ) {
                WalletAvatar(
                    name = sender.name.ifBlank { sender.username },
                    avatar = sender.avatar,
                    homeGateway = homeGateway,
                    size = 20.dp
                )
            }
        }

        if (recipient != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
            ) {
                WalletAvatar(
                    name = recipient.name.ifBlank { recipient.username },
                    avatar = recipient.avatar,
                    homeGateway = homeGateway,
                    size = 20.dp
                )
            }
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ElementUiPalette.Block)
                .border(
                    width = 1.dp,
                    color = ElementUiPalette.scaledBorder(0.92f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (giftBitmap != null) {
                Image(
                    bitmap = giftBitmap!!,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = ElementUiPalette.Accent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun WalletTransferTransactionIcon(
    transaction: WalletTransaction,
    homeGateway: HomeGateway
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(ElementUiPalette.BlockSoft),
        contentAlignment = Alignment.Center
    ) {
        transaction.sender?.let { sender ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-2).dp, y = (-2).dp)
            ) {
                WalletAvatar(
                    name = sender.name.ifBlank { sender.username },
                    avatar = sender.avatar,
                    homeGateway = homeGateway,
                    size = 20.dp
                )
            }
        }

        transaction.recipient?.let { recipient ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
            ) {
                WalletAvatar(
                    name = recipient.name.ifBlank { recipient.username },
                    avatar = recipient.avatar,
                    homeGateway = homeGateway,
                    size = 20.dp
                )
            }
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(ElementUiPalette.Block),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowOutward,
                contentDescription = null,
                tint = ElementUiPalette.Accent,
                modifier = Modifier
                    .size(14.dp)
                    .rotate(if (transaction.incoming) 180f else 0f)
            )
        }
    }
}

@Composable
private fun WalletEarnInfo() {
    val items = listOf(
        Triple(Icons.Default.Description, "Пост", "0.005"),
        Triple(Icons.Default.ModeComment, "Комментарий", "0.003"),
        Triple(Icons.Default.LibraryMusic, "Трек", "0.005")
    )

    UIKit.Block {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items.forEach { (icon, title, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ElementUiPalette.BlockSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ElementUiPalette.Accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = ElementUiPalette.TextPrimary,
                            fontSize = 14.sp
                        )
                        Row(
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElementUiPalette.BlockSoft)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UIKit.Eball(size = 20.dp, fontSize = 10.sp)
                            Text(
                                text = amount,
                                color = ElementUiPalette.TextPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Text(
                text = "Начисления зависят от активности и проверки антиспама.",
                color = ElementUiPalette.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 5.dp, start = 4.dp, end = 4.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun WalletSubscriptionTab(
    subscriptionStatus: Boolean,
    history: List<AuthGoldHistory>,
    actionLoading: Boolean,
    onPay: () -> Unit,
    onActivate: (String) -> Unit
) {
    var activateDialogOpen by remember { mutableStateOf(false) }
    var activateCode by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!subscriptionStatus) {
            UIKit.Block(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_profile_gold),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(74.dp)
                    )
                    Text(
                        text = "1 месяц / 0.1 е-балл",
                        color = ElementUiPalette.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UIKit.Button(
                            title = "Активировать",
                            onClick = { activateDialogOpen = true },
                            enabled = !actionLoading,
                            modifier = Modifier.weight(1f),
                            variant = ElementButtonVariant.Soft
                        )
                        UIKit.Button(
                            title = "Оплатить",
                            onClick = onPay,
                            enabled = !actionLoading,
                            loading = actionLoading,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        UIKit.Block(modifier = Modifier.fillMaxWidth()) {
            if (history.isEmpty()) {
                Text(
                    text = "История подписки пока пуста",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    history.forEach { item ->
                        WalletSubscriptionHistoryRow(item = item)
                    }
                }
            }
        }
    }

    if (activateDialogOpen) {
        Dialog(
            onDismissRequest = {
                if (!actionLoading) {
                    activateDialogOpen = false
                }
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                UIKit.Block(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Введите ключ",
                            color = ElementUiPalette.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ключ можно получить разными способами. Начиная от покупки, заканчивая просто подарком от кого-то.",
                            color = ElementUiPalette.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )
                        UIKit.Input(
                            value = activateCode,
                            onValueChange = { activateCode = it },
                            placeholder = "Код активации",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UIKit.Button(
                                title = "Закрыть",
                                onClick = { activateDialogOpen = false },
                                enabled = !actionLoading,
                                modifier = Modifier.weight(1f),
                                variant = ElementButtonVariant.Soft
                            )
                            UIKit.Button(
                                title = "Активировать",
                                onClick = {
                                    onActivate(activateCode)
                                    activateCode = ""
                                    activateDialogOpen = false
                                },
                                enabled = activateCode.trim().isNotBlank() && !actionLoading,
                                loading = actionLoading,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletSubscriptionHistoryRow(item: AuthGoldHistory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.BlockSoft)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (item.status == 1) "Активна" else "Неактивна",
            color = ElementUiPalette.Accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = ", активировано ${formatWalletHistoryDate(item.date)}",
            color = ElementUiPalette.TextPrimary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WalletReferralTab(
    loading: Boolean,
    dashboard: WalletReferralDashboard?,
    history: List<WalletReferralHistoryItem>,
    hasMore: Boolean,
    loadingMore: Boolean,
    homeGateway: HomeGateway,
    onOpenProfile: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    if (loading && dashboard == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            UIKit.Loader(size = 28)
        }
        return
    }

    if (dashboard == null) {
        ErrorItem("Не удалось загрузить реферальную статистику")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        UIKit.Block {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Реферальная программа",
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Приглашайте друзей и получайте E-баллы за их активность.",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 13.sp
                )

                WalletCopyField("Реферальный код", dashboard.refCode)
                WalletCopyField("Ссылка-приглашение", dashboard.inviteLink)

                WalletStatRow("Приглашено", dashboard.totalInvited.toString())
                WalletStatRow("С наградой", dashboard.rewarded.toString())
                WalletStatRow("В ожидании", dashboard.pending.toString())
                WalletStatRow(
                    "Заработано",
                    buildString { append(formatEballs(dashboard.totalEarned)); append(" "); append("E") }
                )
                dashboard.invitedByUsername?.takeIf { it.isNotBlank() }?.let { inviter ->
                    Text(
                        text = "Вас пригласил: @$inviter",
                        color = ElementUiPalette.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        UIKit.Block {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "История приглашений",
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (history.isEmpty()) {
                    Text(
                        text = "История пока пуста",
                        color = ElementUiPalette.TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    )
                } else {
                    history.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElementUiPalette.BlockSoft)
                                .padding(horizontal = 7.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val invited = item.invited
                            if (invited != null) {
                                WalletAvatar(
                                    name = invited.name,
                                    avatar = invited.avatar,
                                    homeGateway = homeGateway,
                                    size = 32.dp
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "@${invited.username}",
                                        color = ElementUiPalette.TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${translateReferralStatus(item.status)} · ${formatTimeAge(item.rewardDate ?: item.createdAt.orEmpty())}",
                                        color = ElementUiPalette.TextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = "Пользователь",
                                    color = ElementUiPalette.TextPrimary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            val openUsername = invited?.username?.takeIf { it.isNotBlank() }
                            if (openUsername != null) {
                                Text(
                                    text = "Открыть",
                                    color = ElementUiPalette.Accent,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { onOpenProfile(openUsername) }
                                )
                            }
                            Text(
                                text = formatEballs(item.rewardInviter),
                                color = ElementUiPalette.Accent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            UIKit.Eball(size = 20.dp, fontSize = 10.sp)
                        }
                    }
                }

                if (hasMore && history.isNotEmpty()) {
                    UIKit.Button(
                        title = if (loadingMore) "..." else "Показать еще",
                        onClick = onLoadMore,
                        enabled = !loadingMore,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletCopyField(label: String, value: String) {
    val clipboard = LocalClipboardManager.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = ElementUiPalette.TextSecondary,
            fontSize = 13.sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ElementUiPalette.BlockSoft)
                .clickable(enabled = value.isNotBlank()) {
                    clipboard.setText(AnnotatedString(value))
                }
                .padding(horizontal = 9.dp, vertical = 7.dp)
        ) {
            Text(
                text = value.ifBlank { "—" },
                color = ElementUiPalette.TextPrimary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun WalletStatRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.BlockSoft)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ElementUiPalette.TextPrimary,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = ElementUiPalette.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WalletTransferModal(
    walletGateway: WalletGateway,
    homeGateway: HomeGateway,
    balance: Double,
    onBalanceChanged: (Double) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<WalletUser?>(null) }
    var users by remember { mutableStateOf<List<WalletUser>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val amount = parsePositiveDouble(amountInput)
    val fee = amount * 0.1
    val total = amount + fee
    val balanceAfter = balance - total
    val canSend = selectedUser != null && amount > 0.0 && balanceAfter >= 0.0 && !isSending

    LaunchedEffect(query, selectedUser?.id) {
        if (selectedUser != null) return@LaunchedEffect
        searchJob?.cancel()
        if (query.trim().length < 2) {
            users = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        searchJob = scope.launch {
            isSearching = true
            delay(180)
            users = runCatching { walletGateway.searchUsers(query.trim()) }.getOrDefault(emptyList())
            isSearching = false
        }
    }

    fun sendTransfer() {
        val user = selectedUser ?: return
        if (!canSend) return
        isSending = true
        localError = null
        scope.launch {
            val result = runCatching {
                walletGateway.sendEballs(
                    recipientId = user.id,
                    amount = amount,
                    message = messageInput.trim().ifBlank { null }
                )
            }.getOrNull()
            if (result?.isSuccess == true) {
                val deducted = result.totalDeducted ?: total
                onBalanceChanged(balance - deducted)
                completed = true
            } else {
                localError = result?.message ?: "Не удалось выполнить перевод"
            }
            isSending = false
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        UIKit.RoutedModal(
            title = "Перевод E-баллов",
            onClose = onClose
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(
                        id = if (completed) R.drawable.elira_send_end else R.drawable.elira_send
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(164.dp),
                    contentScale = ContentScale.Fit
                )

                if (!completed) {
                    UIKit.Block(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Баланс",
                                    color = ElementUiPalette.TextSecondary,
                                    fontSize = 14.sp
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatEballs(balance),
                                        color = ElementUiPalette.TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    UIKit.Eball(size = 22.dp, fontSize = 11.sp)
                                }
                            }

                            if (selectedUser == null) {
                                UIKit.Input(
                                    value = query,
                                    onValueChange = { query = it },
                                    placeholder = "Получатель",
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (isSearching) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        UIKit.Loader(size = 22)
                                    }
                                } else if (users.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        users.forEach { user ->
                                            WalletUserRow(
                                                user = user,
                                                homeGateway = homeGateway,
                                                onClick = {
                                                    selectedUser = user
                                                    users = emptyList()
                                                    query = ""
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                WalletSelectedUserRow(
                                    user = selectedUser!!,
                                    homeGateway = homeGateway,
                                    onRemove = { selectedUser = null }
                                )
                            }

                            UIKit.Input(
                                value = amountInput,
                                onValueChange = { amountInput = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                                placeholder = "Сумма перевода",
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (selectedUser != null && amount > 0.0) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ElementUiPalette.BlockSoft)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    WalletTransferDetailRow("Сумма", amount)
                                    WalletTransferDetailRow("Комиссия (10%)", fee)
                                    WalletTransferDetailRow("После перевода", balanceAfter)
                                }
                            }

                            UIKit.Input(
                                value = messageInput,
                                onValueChange = { messageInput = it },
                                placeholder = "Сообщение",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 36.dp, max = 140.dp),
                                singleLine = false
                            )

                            if (!localError.isNullOrBlank()) {
                                Text(
                                    text = localError.orEmpty(),
                                    color = ElementUiPalette.Error,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    UIKit.Button(
                        title = if (isSending) "Отправка..." else "Отправить",
                        onClick = ::sendTransfer,
                        enabled = canSend,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    UIKit.Block(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Перевод выполнен",
                            color = ElementUiPalette.TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UIKit.Button(
                            title = "Закрыть",
                            onClick = onClose,
                            modifier = Modifier.weight(1f)
                        )
                        UIKit.Button(
                            title = "Еще перевод",
                            onClick = {
                                completed = false
                                amountInput = ""
                                messageInput = ""
                                selectedUser = null
                                query = ""
                                users = emptyList()
                                localError = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletTransferDetailRow(
    title: String,
    value: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = ElementUiPalette.TextSecondary,
            fontSize = 13.sp
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatEballs(value),
                color = if (title == "После перевода" && value < 0.0) ElementUiPalette.Error else ElementUiPalette.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            UIKit.Eball(size = 20.dp, fontSize = 10.sp)
        }
    }
}

@Composable
private fun WalletUserRow(
    user: WalletUser,
    homeGateway: HomeGateway,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.BlockSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WalletAvatar(
            name = user.name,
            avatar = user.avatar,
            homeGateway = homeGateway,
            size = 30.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                color = ElementUiPalette.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.username}",
                color = ElementUiPalette.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WalletSelectedUserRow(
    user: WalletUser,
    homeGateway: HomeGateway,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WalletUserRow(
            user = user,
            homeGateway = homeGateway,
            onClick = {}
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ElementUiPalette.BlockSoft)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Убрать",
                tint = ElementUiPalette.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun WalletAvatar(
    name: String,
    avatar: PostImageAsset?,
    homeGateway: HomeGateway,
    size: androidx.compose.ui.unit.Dp
) {
    val bitmap by rememberWalletImageBitmap(avatar, homeGateway)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF7F6EB0), Color(0xFFA19DB1))))
            .border(1.dp, ElementUiPalette.Block, CircleShape),
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
                fontSize = (size.value * 0.34f).sp
            )
        }
    }
}

private fun formatEballs(value: Double): String {
    return String.format(Locale.US, "%.3f", value)
}

private fun formatWalletHistoryDate(value: String?): String {
    val raw = value?.takeIf { it.isNotBlank() } ?: return "неизвестно"
    return runCatching {
        OffsetDateTime.parse(raw).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }.getOrElse {
        raw.take(10)
    }
}

private fun parsePositiveDouble(raw: String): Double {
    val value = raw.trim().replace(',', '.')
    val parsed = value.toDoubleOrNull() ?: return 0.0
    return if (parsed > 0.0) parsed else 0.0
}

private fun translateReferralStatus(status: String): String {
    return when (status.lowercase()) {
        "rewarded" -> "Награда выдана"
        "pending_activity" -> "Ждет активность"
        "rejected" -> "Отклонено"
        else -> status.ifBlank { "Неизвестно" }
    }
}

@Composable
private fun rememberWalletImageBitmap(
    avatar: PostImageAsset?,
    homeGateway: HomeGateway
) = produceState<ImageBitmap?>(initialValue = null, key1 = avatar?.cacheKey) {
    val target = avatar ?: return@produceState
    val bytes = runCatching { homeGateway.loadImageBytes(target) }.getOrNull() ?: return@produceState
    if (bytes.isEmpty()) return@produceState
    value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}

@Composable
private fun ErrorItem(text: String) {
    UIKit.Block {
        Text(
            text = text,
            color = ElementUiPalette.Error,
            fontSize = 13.sp
        )
    }
}
