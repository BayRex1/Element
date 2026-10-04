package elemsocial.com.ui.pack.components.posts

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import elemsocial.com.domain.model.PollVoteResult
import elemsocial.com.domain.model.PostPoll
import elemsocial.com.domain.model.PostPollOption
import elemsocial.com.ui.pack.theme.ElementUiPalette
import java.time.OffsetDateTime
import kotlinx.coroutines.launch

@Composable
fun ElementPostPoll(
    postId: Int,
    poll: PostPoll,
    onVote: suspend (List<Int>) -> PollVoteResult,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var data by remember(poll) { mutableStateOf(poll) }
    var voting by remember(poll.id) { mutableStateOf(false) }
    var resultsOpen by remember(poll.id) { mutableStateOf(false) }
    var errorText by remember(poll.id) { mutableStateOf<String?>(null) }
    val optionVotes = remember(poll.id) { mutableStateMapOf<Int, Int>() }

    val hasVoted = data.userVote.isNotEmpty()
    val showResults = hasVoted
    val isExpired = remember(data.expiresAt) { data.expiresAt.isExpiredPollDate() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 220)),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (data.question.isNotBlank()) {
            Text(
                text = data.question,
                color = ElementUiPalette.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.options.forEach { option ->
                val selected = data.userVote.contains(option.id)
                val votes = optionVotes[option.id] ?: option.votesCount
                val percent = if (data.totalVotes > 0) {
                    ((votes.toFloat() / data.totalVotes.toFloat()) * 100f).toInt()
                } else {
                    0
                }

                PollOptionRow(
                    option = option,
                    selected = selected,
                    multiple = data.multipleChoice,
                    percent = percent,
                    showResults = showResults,
                    enabled = !voting && !isExpired,
                    onClick = {
                        val optionIds = resolvePollVote(data, option.id)
                        voting = true
                        errorText = null
                        scope.launch {
                            val result = runCatching { onVote(optionIds) }.getOrNull()
                            if (result?.isSuccess == true && result.poll != null) {
                                val updatedPoll = data.copy(
                                    totalVotes = result.poll.totalVotes,
                                    userVote = result.poll.userVote,
                                    options = result.poll.options
                                )
                                data = updatedPoll
                                updatedPoll.options.forEach { updated ->
                                    optionVotes[updated.id] = updated.votesCount
                                }
                                if (updatedPoll.userVote.isEmpty()) {
                                    resultsOpen = false
                                }
                            } else {
                                errorText = result?.message ?: "Не удалось проголосовать"
                            }
                            voting = false
                        }
                    }
                )
            }
        }

        PollFooter(
            totalVotes = data.totalVotes,
            anonymous = data.isAnonymous,
            multiple = data.multipleChoice,
            expired = isExpired,
            canShowResults = showResults,
            resultsOpen = resultsOpen,
            onToggleResults = { resultsOpen = !resultsOpen }
        )

        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText.orEmpty(),
                color = ElementUiPalette.Error,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        if (resultsOpen) {
            PollResultsPanel(data = data)
        }
    }
}

@Composable
private fun PollOptionRow(
    option: PostPollOption,
    selected: Boolean,
    multiple: Boolean,
    percent: Int,
    showResults: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val textColor = if (selected) ElementUiPalette.Accent else ElementUiPalette.TextPrimary
    val barColor = ElementUiPalette.Accent.copy(alpha = if (selected) 0.20f else 0.10f)
    val interactionSource = remember { MutableInteractionSource() }
    val animatedPercent by animateFloatAsState(
        targetValue = percent.coerceIn(0, 100).toFloat(),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        if (showResults) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedPercent / 100f)
                        .background(barColor)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 42.dp)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(if (multiple) RoundedCornerShape(5.dp) else CircleShape)
                    .background(if (selected) ElementUiPalette.Accent else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (selected) {
                            ElementUiPalette.Accent
                        } else {
                            ElementUiPalette.TextPrimary.copy(alpha = 0.30f)
                        },
                        shape = if (multiple) RoundedCornerShape(5.dp) else CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = option.text,
                color = textColor,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            if (showResults) {
                Text(
                    text = "${animatedPercent.toInt()}%",
                    color = textColor.copy(alpha = if (selected) 0.9f else 0.5f),
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.widthIn(min = 34.dp)
                )
            }
        }
    }
}

@Composable
private fun PollFooter(
    totalVotes: Int,
    anonymous: Boolean,
    multiple: Boolean,
    expired: Boolean,
    canShowResults: Boolean,
    resultsOpen: Boolean,
    onToggleResults: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$totalVotes ${decline(totalVotes, "голос", "голоса", "голосов")}",
            color = ElementUiPalette.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 14.sp
        )
        if (anonymous) {
            Text(
                text = "· Анонимный",
                color = ElementUiPalette.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        }
        if (multiple) {
            Text(
                text = "· Несколько ответов",
                color = ElementUiPalette.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        }
        if (expired) {
            Text(
                text = "· Завершён",
                color = ElementUiPalette.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        }
        if (canShowResults) {
            Text(
                text = "Результаты",
                color = ElementUiPalette.Accent.copy(alpha = if (resultsOpen) 1f else 0.75f),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleResults
                    )
                    .padding(horizontal = 2.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PollResultsPanel(data: PostPoll) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ElementUiPalette.scaledBorder(0.08f))
        )
        data.options.forEachIndexed { index, option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = option.text,
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${option.votesCount} ${decline(option.votesCount, "голос", "голоса", "голосов")}",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 14.sp
                )
            }
            if (index != data.options.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ElementUiPalette.scaledBorder(0.08f))
                )
            }
        }
    }
}

private fun resolvePollVote(data: PostPoll, optionId: Int): List<Int> {
    val optionIds = if (data.multipleChoice) {
        if (data.userVote.contains(optionId)) {
            data.userVote.filter { it != optionId }
        } else {
            data.userVote + optionId
        }
    } else {
        listOf(optionId)
    }

    return optionIds.ifEmpty { listOf(optionId) }
}

private fun String?.isExpiredPollDate(): Boolean {
    if (isNullOrBlank()) return false
    return runCatching {
        OffsetDateTime.parse(this).isBefore(OffsetDateTime.now())
    }.getOrDefault(false)
}

private fun decline(value: Int, one: String, two: String, many: String): String {
    val abs = kotlin.math.abs(value) % 100
    val n1 = abs % 10
    return when {
        abs in 11..19 -> many
        n1 == 1 -> one
        n1 in 2..4 -> two
        else -> many
    }
}
