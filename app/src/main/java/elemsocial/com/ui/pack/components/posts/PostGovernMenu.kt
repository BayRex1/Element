package elemsocial.com.ui.pack.components.posts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import elemsocial.com.domain.model.FeedPost
import elemsocial.com.ui.pack.components.modals.ElementContextMenuItem
import elemsocial.com.ui.pack.theme.ElementUiPalette

fun buildPostGovernItems(
    post: FeedPost,
    isAdmin: Boolean,
    onReportPost: () -> Unit,
    onEditPost: () -> Unit,
    onDeleteOrRestorePost: () -> Unit,
    onDeletePostForever: () -> Unit,
    onArchiveToggle: () -> Unit,
    onBlockToggle: () -> Unit
): List<ElementContextMenuItem> {
    val canManage = post.myPost || isAdmin
    val canBlockAuthor = isAdmin && !post.myPost

    return buildList {
        if (post.myPost && !post.deleted) {
            add(
                ElementContextMenuItem(
                    title = "Редактировать",
                    icon = Icons.Default.Edit,
                    onClick = onEditPost
                )
            )
        }
        if (canManage) {
            add(
                ElementContextMenuItem(
                    title = if (post.deleted) "Восстановить" else "Удалить",
                    icon = Icons.Default.Delete,
                    color = ElementUiPalette.Error,
                    onClick = onDeleteOrRestorePost
                )
            )
        }
        if (post.deleted && canManage) {
            add(
                ElementContextMenuItem(
                    title = "Удалить навсегда",
                    icon = Icons.Default.Delete,
                    color = ElementUiPalette.Error,
                    onClick = onDeletePostForever
                )
            )
        }
        if (post.myPost) {
            add(
                ElementContextMenuItem(
                    title = if (post.archived) "Убрать из архива" else "В архив",
                    icon = Icons.Default.Archive,
                    onClick = onArchiveToggle
                )
            )
        }
        if (canBlockAuthor) {
            add(
                ElementContextMenuItem(
                    title = if (post.author?.blocked == true) "Разблокировать" else "Заблокировать",
                    icon = Icons.Default.Block,
                    color = ElementUiPalette.Error,
                    onClick = onBlockToggle
                )
            )
        }
    }
}
