package elemsocial.com.ui.pack.components.posts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import elemsocial.com.domain.model.PostPoll
import elemsocial.com.domain.model.PostPollOption
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.components.inputs.ElementInputField
import elemsocial.com.ui.pack.theme.ElementUiPalette

@Composable
fun PollCreatorModal(
    onClose: () -> Unit,
    onSave: (PostPoll) -> Unit
) {
    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var isAnonymous by remember { mutableStateOf(true) }
    var isMultiple by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val validOptions = options.map { it.trim() }.filter { it.isNotEmpty() }
    val canSave = validOptions.size >= 2
    val sectionShape = RoundedCornerShape(12.dp)
    val inputShape = RoundedCornerShape(10.dp)
    val sectionBorderColor = ElementUiPalette.scaledBorder(0.85f)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun handleSave() {
        if (!canSave) return
        onSave(
            PostPoll(
                id = 0,
                question = question.trim(),
                isAnonymous = isAnonymous,
                multipleChoice = isMultiple,
                options = validOptions.mapIndexed { index, text ->
                    PostPollOption(id = index, text = text, votesCount = 0)
                }
            )
        )
    }

    fun updateOption(index: Int, value: String) {
        if (index < options.size) {
            options[index] = value
        }
    }

    fun removeOption(index: Int) {
        if (options.size > 2) {
            options.removeAt(index)
        }
    }

    fun addOption() {
        if (options.size < 10) {
            options.add("")
        }
    }

    UIKit.RoutedModal(
        title = "Создание опроса",
        onClose = onClose,
        headerHeight = 48.dp,
        titleFontWeight = FontWeight.Medium
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Вопрос",
                color = ElementUiPalette.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 2.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(sectionShape)
                    .background(ElementUiPalette.Block)
                    .border(1.dp, sectionBorderColor, sectionShape)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                ElementInputField(
                    value = question,
                    onValueChange = { question = it.take(200) },
                    placeholder = "О чём хотите спросить? (необязательно)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    containerColor = ElementUiPalette.BlockSoft,
                    borderWidth = 1.dp,
                    borderColor = sectionBorderColor,
                    shape = inputShape,
                    textFontSize = 15.sp,
                    textLineHeight = 18.sp,
                    placeholderFontSize = 15.sp,
                    minHeight = 36.dp,
                    contentHorizontalPadding = 10.dp,
                    contentVerticalPadding = 0.dp
                )
            }

                Text(
                    text = "Варианты ответа · ${validOptions.size}/10",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(sectionShape)
                        .background(ElementUiPalette.Block)
                        .border(1.dp, sectionBorderColor, sectionShape)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    options.forEachIndexed { index, option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ElementInputField(
                                value = option,
                                onValueChange = { updateOption(index, it.take(100)) },
                                placeholder = "Вариант ${index + 1}",
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                containerColor = ElementUiPalette.BlockSoft,
                                borderWidth = 1.dp,
                                borderColor = sectionBorderColor,
                                shape = inputShape,
                                textFontSize = 14.sp,
                                textLineHeight = 17.sp,
                                placeholderFontSize = 14.sp,
                                minHeight = 36.dp,
                                contentHorizontalPadding = 10.dp,
                                contentVerticalPadding = 0.dp
                            )
                            if (options.size > 2) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(ElementUiPalette.Block)
                                        .clickable { removeOption(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = ElementCloseIcon,
                                        contentDescription = null,
                                        tint = ElementUiPalette.TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (options.size < 10) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElementUiPalette.BlockSoft)
                                .border(1.dp, sectionBorderColor, RoundedCornerShape(8.dp))
                                .clickable { addOption() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = ElementPlusIcon,
                                    contentDescription = null,
                                    tint = ElementUiPalette.Accent,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "Добавить вариант",
                                    color = ElementUiPalette.Accent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (!canSave) {
                        Text(
                            text = "Минимум 2 заполненных варианта",
                            color = ElementUiPalette.TextSecondary.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Настройки",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(sectionShape)
                        .background(ElementUiPalette.Block)
                        .border(1.dp, sectionBorderColor, sectionShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SwitchRow(
                        title = "Анонимный опрос",
                        value = isAnonymous,
                        onChange = { isAnonymous = it }
                    )
                    SwitchRow(
                        title = "Несколько вариантов ответа",
                        value = isMultiple,
                        onChange = { isMultiple = it }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (canSave) ElementUiPalette.Accent else ElementUiPalette.Border)
                        .clickable(enabled = canSave) { handleSave() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Сохранить",
                        color = if (canSave) Color.White else ElementUiPalette.TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    value: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = ElementUiPalette.TextPrimary,
            fontSize = 14.sp
        )
        Row(
            modifier = Modifier
                .width(44.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(if (value) ElementUiPalette.Accent else ElementUiPalette.Border)
                .clickable { onChange(!value) }
                .padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (value) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun AttachedPollPreview(
    poll: PostPoll,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.Accent.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = ElementPollIcon,
            contentDescription = null,
            tint = ElementUiPalette.Accent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = buildString {
                append("Опрос · ")
                append(poll.options.size)
                append(' ')
                append(decline(poll.options.size, "вариант", "варианта", "вариантов"))
                if (poll.question.isNotBlank()) {
                    append(" · «")
                    append(poll.question.take(30))
                    if (poll.question.length > 30) append("…")
                    append("»")
                }
            },
            color = ElementUiPalette.TextPrimary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ElementCloseIcon,
                contentDescription = "Удалить",
                tint = ElementUiPalette.TextSecondary,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

private val ElementPlusIcon: ImageVector = ImageVector.Builder(
    name = "ElementPlusIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Black)
    ) {
        moveTo(19f, 11f)
        horizontalLineTo(13f)
        verticalLineTo(5f)
        arcTo(1f, 1f, 0f, false, false, 11f, 5f)
        verticalLineTo(11f)
        horizontalLineTo(5f)
        arcTo(1f, 1f, 0f, false, false, 5f, 13f)
        horizontalLineTo(11f)
        verticalLineTo(19f)
        arcTo(1f, 1f, 0f, false, false, 13f, 19f)
        verticalLineTo(13f)
        horizontalLineTo(19f)
        arcTo(1f, 1f, 0f, false, false, 19f, 11f)
    }
}.build()

private val ElementCloseIcon: ImageVector = ImageVector.Builder(
    name = "ElementCloseIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Black)
    ) {
        moveTo(6.2253f, 4.81108f)
        curveTo(5.83477f, 4.42056f, 5.20161f, 4.42056f, 4.81108f, 4.81108f)
        curveTo(4.42056f, 5.20161f, 4.42056f, 5.83477f, 4.81108f, 6.2253f)
        lineTo(10.5858f, 12f)
        lineTo(4.81114f, 17.7747f)
        curveTo(4.42062f, 18.1652f, 4.42062f, 18.7984f, 4.81114f, 19.1889f)
        curveTo(5.20167f, 19.5794f, 5.83483f, 19.5794f, 6.22535f, 19.1889f)
        lineTo(12f, 13.4142f)
        lineTo(17.7747f, 19.1889f)
        curveTo(18.1652f, 19.5794f, 18.7984f, 19.5794f, 19.1889f, 19.1889f)
        curveTo(19.5794f, 18.7984f, 19.5794f, 18.1652f, 19.1889f, 17.7747f)
        lineTo(13.4142f, 12f)
        lineTo(19.189f, 6.2253f)
        curveTo(19.5795f, 5.83477f, 19.5795f, 5.20161f, 19.189f, 4.81108f)
        curveTo(18.7985f, 4.42056f, 18.1653f, 4.42056f, 17.7748f, 4.81108f)
        lineTo(12f, 10.5858f)
        lineTo(6.2253f, 4.81108f)
    }
}.build()

private val ElementPollIcon: ImageVector = ImageVector.Builder(
    name = "ElementPollIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Black)
    ) {
        moveTo(5f, 9f)
        horizontalLineTo(7f)
        verticalLineTo(20f)
        horizontalLineTo(5f)
        verticalLineTo(9f)
        moveTo(9f, 4f)
        horizontalLineTo(11f)
        verticalLineTo(20f)
        horizontalLineTo(9f)
        verticalLineTo(4f)
        moveTo(13f, 11f)
        horizontalLineTo(15f)
        verticalLineTo(20f)
        horizontalLineTo(13f)
        verticalLineTo(11f)
        moveTo(17f, 8f)
        horizontalLineTo(19f)
        verticalLineTo(20f)
        horizontalLineTo(17f)
        verticalLineTo(8f)
    }
}.build()

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
