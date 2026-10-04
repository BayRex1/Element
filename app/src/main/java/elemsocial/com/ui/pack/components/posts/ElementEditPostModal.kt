package elemsocial.com.ui.pack.components.posts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.FeedPost
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.components.inputs.ElementInputField
import elemsocial.com.ui.pack.theme.ElementUiPalette
import kotlinx.coroutines.launch

@Composable
fun ElementEditPostModal(
    post: FeedPost,
    onClose: () -> Unit,
    onSaveRequest: suspend (String) -> ActionResult,
    onSaved: (FeedPost) -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val originalText = remember(post.id, post.text) { post.text.orEmpty() }

    var text by remember(post.id, post.text) { mutableStateOf(originalText) }
    var loading by remember(post.id) { mutableStateOf(false) }
    var errorText by remember(post.id) { mutableStateOf<String?>(null) }

    val hasChanges = text.trim() != originalText.trim()
    val canSave = hasChanges && text.trim().isNotEmpty() && !loading

    fun handleSave() {
        if (!canSave) return

        loading = true
        errorText = null
        scope.launch {
            val result = runCatching { onSaveRequest(text) }.getOrNull()
            loading = false

            if (result?.isSuccess == true) {
                onSaved(
                    post.copy(
                        text = text,
                        editedAt = System.currentTimeMillis().toString()
                    )
                )
                onClose()
            } else {
                errorText = result?.message ?: "Не удалось сохранить изменения"
            }
        }
    }

    LaunchedEffect(post.id) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        UIKit.RoutedModal(
            title = "Редактировать пост",
            onClose = onClose,
            headerHeight = 48.dp,
            titleFontWeight = FontWeight.Medium
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElementInputField(
                    value = text,
                    onValueChange = {
                        text = it.take(30000)
                        errorText = null
                    },
                    placeholder = "Текст поста...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp, max = 220.dp)
                        .focusRequester(focusRequester),
                    singleLine = false,
                    containerColor = ElementUiPalette.BlockSoft,
                    textFontSize = 16.sp,
                    textLineHeight = 20.sp,
                    placeholderFontSize = 16.sp,
                    minHeight = 72.dp,
                    contentHorizontalPadding = 10.dp,
                    contentVerticalPadding = 8.dp
                )

                if (!errorText.isNullOrBlank()) {
                    androidx.compose.material3.Text(
                        text = errorText.orEmpty(),
                        color = ElementUiPalette.Error,
                        fontSize = 13.sp
                    )
                }

                UIKit.Button(
                    title = "Сохранить",
                    onClick = ::handleSave,
                    enabled = canSave,
                    loading = loading,
                    textFontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
