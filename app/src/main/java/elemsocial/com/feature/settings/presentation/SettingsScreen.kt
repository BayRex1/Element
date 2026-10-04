package elemsocial.com.feature.settings.presentation

import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.annotation.DrawableRes
import elemsocial.com.R
import elemsocial.com.BuildConfig
import elemsocial.com.core.settings.AppThemeMode
import elemsocial.com.core.model.TransparencyMode
import elemsocial.com.domain.model.ActionResult
import elemsocial.com.domain.model.AuthAccountChannel
import elemsocial.com.domain.model.AuthSession
import elemsocial.com.domain.model.PostImageAsset
import elemsocial.com.domain.model.PostsCategory
import elemsocial.com.domain.model.Profile
import elemsocial.com.domain.model.ProfileLink
import elemsocial.com.feature.auth.presentation.AuthGateway
import elemsocial.com.feature.home.presentation.HomeGateway
import elemsocial.com.feature.profile.presentation.ProfileGateway
import elemsocial.com.feature.profile.presentation.ProfileLinkIcon
import elemsocial.com.ui.pack.UIKit
import elemsocial.com.ui.pack.components.buttons.ElementButtonVariant
import elemsocial.com.ui.pack.theme.ElementUiPalette
import com.caverock.androidsvg.SVG
import elemsocial.com.core.cache.ImageDiskCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

private data class SettingsMenuEntry(
    val type: String,
    val label: String,
    @param:DrawableRes val iconRes: Int,
    val color: Color
)

private enum class ProfileDeleteTarget {
    Avatar,
    Cover
}

private enum class StorageUiCategoryId {
    Avatars,
    Covers,
    Music,
    Posts,
    Comments
}

private data class StorageUiCategory(
    val id: StorageUiCategoryId,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val sizeBytes: Long = 0L,
    val selected: Boolean = true
)

private enum class StorageClearDialogTarget {
    Selected,
    All
}

private enum class DeleteAccountStep {
    ConfirmEmail,
    ConfirmPassword,
    ConfirmCode
}

private data class SettingsLinkEditorRequest(
    val link: ProfileLink? = null
)

private val SettingsModalPrimaryTextSize = 16.sp
private val SettingsModalSecondaryTextSize = 13.sp
private val SettingsModalSupportTextSize = 12.sp
private val SettingsModalSectionTitleTextSize = 13.sp

private val StorageMenuIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "StorageMenuIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(2f, 20f)
            horizontalLineTo(22f)
            verticalLineTo(16f)
            horizontalLineTo(2f)
            close()

            moveTo(4f, 17f)
            horizontalLineTo(6f)
            verticalLineTo(19f)
            horizontalLineTo(4f)
            close()

            moveTo(2f, 4f)
            verticalLineTo(8f)
            horizontalLineTo(22f)
            verticalLineTo(4f)
            close()

            moveTo(6f, 7f)
            horizontalLineTo(4f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            close()

            moveTo(2f, 14f)
            horizontalLineTo(22f)
            verticalLineTo(10f)
            horizontalLineTo(2f)
            close()

            moveTo(4f, 11f)
            horizontalLineTo(6f)
            verticalLineTo(13f)
            horizontalLineTo(4f)
            close()
        }
    }.build()
}

private val StorageCategoriesTemplate = listOf(
    StorageUiCategory(
        id = StorageUiCategoryId.Avatars,
        title = "Аватары",
        icon = Icons.Filled.AccountCircle,
        color = Color(0xFFFFAB49)
    ),
    StorageUiCategory(
        id = StorageUiCategoryId.Covers,
        title = "Обложки",
        icon = Icons.Filled.Image,
        color = Color(0xFF4BAF78)
    ),
    StorageUiCategory(
        id = StorageUiCategoryId.Music,
        title = "Музыка",
        icon = Icons.Filled.MusicNote,
        color = Color(0xFF8B5CF6)
    ),
    StorageUiCategory(
        id = StorageUiCategoryId.Posts,
        title = "Посты",
        icon = Icons.Filled.Photo,
        color = Color(0xFF5B8AF7)
    ),
    StorageUiCategory(
        id = StorageUiCategoryId.Comments,
        title = "Комментарии",
        icon = Icons.Filled.Comment,
        color = Color(0xFFF75BCF)
    )
)

private fun StorageUiCategoryId.toCacheCategory(): ImageDiskCache.StorageCategory {
    return when (this) {
        StorageUiCategoryId.Avatars -> ImageDiskCache.StorageCategory.Avatars
        StorageUiCategoryId.Covers -> ImageDiskCache.StorageCategory.Covers
        StorageUiCategoryId.Music -> ImageDiskCache.StorageCategory.Music
        StorageUiCategoryId.Posts -> ImageDiskCache.StorageCategory.Posts
        StorageUiCategoryId.Comments -> ImageDiskCache.StorageCategory.Comments
    }
}

private fun normalizeUsername(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isBlank()) return null

    return trimmed
        .removePrefix("@")
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .trim()
        .takeIf { it.isNotBlank() }
}

private val AccountButtons = listOf<SettingsMenuEntry>(
    SettingsMenuEntry(
        type = "change_username",
        label = "Уникальное имя",
        iconRes = R.drawable.ic_settings_username,
        color = Color(0xFF472ADD)
    )
)

private val ConfidentialityButtons = listOf(
    SettingsMenuEntry(
        type = "sessions",
        label = "Сессии",
        iconRes = R.drawable.ic_settings_sessions,
        color = Color(0xFF0CE300)
    ),
    SettingsMenuEntry(
        type = "change_email",
        label = "Сменить почту",
        iconRes = R.drawable.ic_settings_email,
        color = Color(0xFFFF5252)
    ),
    SettingsMenuEntry(
        type = "change_password",
        label = "Сменить пароль",
        iconRes = R.drawable.ic_settings_lock,
        color = Color(0xFF472ADD)
    ),
    SettingsMenuEntry(
        type = "storage",
        label = "Хранилище",
        iconRes = R.drawable.ic_settings_storage,
        color = Color(0xFF59AFFF)
    ),
    SettingsMenuEntry(
        type = "elementum",
        label = "Настройки Elementum",
        iconRes = R.drawable.ic_settings_elementum,
        color = Color(0xFFFFD700)
    ),
    SettingsMenuEntry(
        type = "advanced",
        label = "Расширенные настройки",
        iconRes = R.drawable.ic_settings_advanced,
        color = Color(0xFF59AFFF)
    ),
    SettingsMenuEntry(
        type = "delete_account",
        label = "Удалить аккаунт",
        iconRes = R.drawable.ic_settings_delete,
        color = Color(0xFFFF5D5D)
    ),
    // TODO(next-release): вернуть "Мой статус" после переноса логики статусов.
    // TODO(next-release): вернуть "Язык" после переноса смены языка.
    // TODO(next-release): вернуть "Авторы" после переноса списка авторов.
    // TODO(next-release): вернуть "Мои жалобы" после переноса reports.
    // TODO(next-release): вернуть "Мои апелляции" после переноса appeals.
    // TODO(next-release): вернуть "Информация" после переноса info.
)

@Composable
fun SettingsScreen(
    accountName: String?,
    accountEmail: String?,
    accountUsername: String?,
    accountAvatar: PostImageAsset?,
    homeGateway: HomeGateway,
    authGateway: AuthGateway,
    profileGateway: ProfileGateway,
    themeMode: AppThemeMode,
    defaultFeed: PostsCategory,
    notificationsToastEnabled: Boolean,
    autoVideoDownloadEnabled: Boolean,
    videoAutoplayEnabled: Boolean,
    transparencyMode: TransparencyMode = TransparencyMode.ADAPTIVE,
    onThemeChanged: (AppThemeMode) -> Unit,
    onDefaultFeedChanged: (PostsCategory) -> Unit,
    onNotificationsToastChanged: (Boolean) -> Unit,
    onAutoVideoDownloadChanged: (Boolean) -> Unit,
    onVideoAutoplayChanged: (Boolean) -> Unit,
    onTransparencyModeChanged: (TransparencyMode) -> Unit = {},
    onAccountEmailUpdated: (String) -> Unit = {},
    onAccountUsernameUpdated: (String) -> Unit = {},
    onLogout: () -> Unit,
    channelToEdit: AuthAccountChannel? = null,
    onChannelUpdated: (AuthAccountChannel) -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isChannelMode = channelToEdit?.id != null && !normalizeUsername(channelToEdit?.username).isNullOrBlank()
    val channelId = channelToEdit?.id
    val initialTargetUsername = remember(accountUsername, channelToEdit?.id, channelToEdit?.username) {
        normalizeUsername(channelToEdit?.username ?: accountUsername)
    }
    var sessionsModalOpen by remember { mutableStateOf(false) }
    var storageModalOpen by remember { mutableStateOf(false) }
    var changePasswordModalOpen by remember { mutableStateOf(false) }
    var changeUsernameModalOpen by remember { mutableStateOf(false) }
    var changeEmailModalOpen by remember { mutableStateOf(false) }
    var advancedSettingsModalOpen by remember { mutableStateOf(false) }
    var elementumSettingsModalOpen by remember { mutableStateOf(false) }
    var deleteAccountModalOpen by remember { mutableStateOf(false) }
    var profile by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf<Profile?>(null) }
    var targetUsername by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) {
        mutableStateOf(initialTargetUsername)
    }
    var editedName by remember(accountName, channelToEdit?.id, channelToEdit?.name) {
        mutableStateOf(channelToEdit?.name ?: accountName.orEmpty())
    }
    var editedUsername by remember(channelToEdit?.id, channelToEdit?.username) {
        mutableStateOf(normalizeUsername(channelToEdit?.username).orEmpty())
    }
    var editedDescription by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf("") }
    var coverUploading by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf(false) }
    var avatarUploading by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf(false) }
    var profileSaving by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf(false) }
    var pendingDeleteTarget by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf<ProfileDeleteTarget?>(null) }
    var previewAvatar by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf<ImageBitmap?>(null) }
    var previewCover by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf<ImageBitmap?>(null) }
    var avatarHiddenLocally by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf(false) }
    var avatarRefreshToken by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf(0) }
    var coverRefreshToken by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf(0) }
    var awaitAvatarRemoteRefresh by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf(false) }
    var awaitCoverRemoteRefresh by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) { mutableStateOf(false) }
    var linkEditorRequest by remember(accountUsername, channelToEdit?.id, channelToEdit?.username) {
        mutableStateOf<SettingsLinkEditorRequest?>(null)
    }
    var deletePostsWithAccount by remember { mutableStateOf(false) }
    var deleteAccountStep by remember { mutableStateOf<DeleteAccountStep?>(null) }
    var deleteAccountEmailInput by remember { mutableStateOf("") }
    var deleteAccountPasswordInput by remember { mutableStateOf("") }
    var deleteAccountCodeInput by remember { mutableStateOf("") }
    var deleteAccountProcessing by remember { mutableStateOf(false) }
    var deleteAccountErrorText by remember { mutableStateOf<String?>(null) }
    var changePasswordOldInput by remember { mutableStateOf("") }
    var changePasswordNewInput by remember { mutableStateOf("") }
    var changePasswordCodeInput by remember { mutableStateOf("") }
    var changePasswordCodeStep by remember { mutableStateOf(false) }
    var changePasswordProcessing by remember { mutableStateOf(false) }
    var changePasswordErrorText by remember { mutableStateOf<String?>(null) }
    var changeUsernameInput by remember { mutableStateOf(normalizeUsername(accountUsername).orEmpty()) }
    var changeUsernameProcessing by remember { mutableStateOf(false) }
    var changeUsernameErrorText by remember { mutableStateOf<String?>(null) }
    var changeEmailInput by remember { mutableStateOf(accountEmail.orEmpty()) }
    var changeEmailCodeInput by remember { mutableStateOf("") }
    var changeEmailCodeStep by remember { mutableStateOf(false) }
    var changeEmailProcessing by remember { mutableStateOf(false) }
    var changeEmailErrorText by remember { mutableStateOf<String?>(null) }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    val normalizedAccountEmail = remember(accountEmail) {
        accountEmail
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }
    }
    val normalizedAccountUsername = remember(accountUsername) {
        normalizeUsername(accountUsername)
    }
    LaunchedEffect(accountUsername, channelToEdit?.id, channelToEdit?.username) {
        targetUsername = initialTargetUsername
    }

    LaunchedEffect(targetUsername) {
        val username = normalizeUsername(targetUsername)
        if (username.isNullOrBlank()) {
            profile = null
            return@LaunchedEffect
        }

        val result = runCatching { profileGateway.loadProfile(username) }.getOrNull()
        profile = if (result?.isSuccess == true) result.profile else null
    }

    LaunchedEffect(
        profile?.id,
        profile?.name,
        profile?.username,
        profile?.description,
        accountName,
        channelToEdit?.id,
        channelToEdit?.name,
        channelToEdit?.username
    ) {
        editedName = profile?.name ?: channelToEdit?.name ?: accountName.orEmpty()
        if (isChannelMode) {
            editedUsername = normalizeUsername(profile?.username ?: channelToEdit?.username).orEmpty()
        }
        editedDescription = profile?.description.orEmpty()
    }

    val shownAvatar = when {
        avatarHiddenLocally -> null
        isChannelMode -> profile?.avatar ?: channelToEdit?.avatar
        else -> profile?.avatar ?: accountAvatar
    }
    val shownName = profile?.name?.takeIf { it.isNotBlank() }
        ?: channelToEdit?.name?.takeIf { it.isNotBlank() }
        ?: accountName?.takeIf { it.isNotBlank() }
        ?: if (isChannelMode) "Канал" else "Пользователь"
    val baseName = profile?.name ?: channelToEdit?.name ?: accountName.orEmpty()
    val baseUsername = if (isChannelMode) {
        normalizeUsername(profile?.username ?: targetUsername ?: channelToEdit?.username).orEmpty()
    } else {
        ""
    }
    val baseDescription = profile?.description.orEmpty()
    val nameChanged = editedName != baseName
    val usernameChanged = isChannelMode && editedUsername != baseUsername
    val descriptionChanged = editedDescription != baseDescription

    fun buildChannelSnapshot(): AuthAccountChannel? {
        val resolvedId = channelId ?: return null
        val resolvedUsername = normalizeUsername(
            profile?.username
                ?: targetUsername
                ?: editedUsername.takeIf { it.isNotBlank() }
                ?: channelToEdit?.username
        ) ?: return null
        return AuthAccountChannel(
            id = resolvedId,
            name = profile?.name?.takeIf { it.isNotBlank() } ?: editedName.takeIf { it.isNotBlank() },
            username = resolvedUsername,
            avatar = shownAvatar
        )
    }

    fun notifyChannelUpdated() {
        if (!isChannelMode) return
        buildChannelSnapshot()?.let(onChannelUpdated)
    }

    fun showSoon(label: String) {
        Toast.makeText(context, "$label переносим следующим этапом", Toast.LENGTH_SHORT).show()
    }

    fun resetChangeUsernameFlow() {
        changeUsernameInput = normalizedAccountUsername.orEmpty()
        changeUsernameProcessing = false
        changeUsernameErrorText = null
    }

    fun closeChangeUsernameModal() {
        changeUsernameModalOpen = false
        resetChangeUsernameFlow()
    }

    fun openChangeUsernameModal() {
        resetChangeUsernameFlow()
        changeUsernameModalOpen = true
    }

    fun normalizeEmail(raw: String): String {
        return raw.trim().lowercase(Locale.ROOT)
    }

    fun resetChangeEmailFlow() {
        changeEmailInput = normalizedAccountEmail.orEmpty()
        changeEmailCodeInput = ""
        changeEmailCodeStep = false
        changeEmailProcessing = false
        changeEmailErrorText = null
    }

    fun closeChangeEmailModal() {
        changeEmailModalOpen = false
        resetChangeEmailFlow()
    }

    fun openChangeEmailModal() {
        resetChangeEmailFlow()
        changeEmailModalOpen = true
    }

    fun resetDeleteAccountFlow(resetPostsToggle: Boolean) {
        deleteAccountStep = null
        deleteAccountEmailInput = ""
        deleteAccountPasswordInput = ""
        deleteAccountCodeInput = ""
        deleteAccountProcessing = false
        deleteAccountErrorText = null
        if (resetPostsToggle) {
            deletePostsWithAccount = false
        }
    }

    fun closeDeleteAccountModal() {
        deleteAccountModalOpen = false
        resetDeleteAccountFlow(resetPostsToggle = false)
    }

    fun resetChangePasswordFlow() {
        changePasswordOldInput = ""
        changePasswordNewInput = ""
        changePasswordCodeInput = ""
        changePasswordCodeStep = false
        changePasswordProcessing = false
        changePasswordErrorText = null
        showOldPassword = false
        showNewPassword = false
    }

    fun closeChangePasswordModal() {
        changePasswordModalOpen = false
        resetChangePasswordFlow()
    }

    fun submitChangeUsername() {
        val normalizedUsername = normalizeUsername(changeUsernameInput)
        if (changeUsernameProcessing) return
        if (normalizedUsername.isNullOrBlank()) {
            changeUsernameErrorText = "Введите username"
            return
        }
        if (normalizedUsername == normalizedAccountUsername) return

        changeUsernameProcessing = true
        changeUsernameErrorText = null
        scope.launch {
            val result = runCatching {
                authGateway.changeUsername(normalizedUsername)
            }.getOrNull() ?: ActionResult(
                status = "error",
                message = "Не удалось изменить username"
            )

            if (result.isSuccess) {
                targetUsername = normalizedUsername
                profile = profile?.copy(username = normalizedUsername)
                onAccountUsernameUpdated(normalizedUsername)
                val reloadedProfile = runCatching {
                    profileGateway.loadProfile(normalizedUsername)
                }.getOrNull()
                if (reloadedProfile?.isSuccess == true) {
                    profile = reloadedProfile.profile
                }
                closeChangeUsernameModal()
                Toast.makeText(
                    context,
                    result.message ?: "Ваше уникальное имя изменено",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                changeUsernameErrorText = result.message ?: "Не удалось изменить username"
                changeUsernameProcessing = false
            }
        }
    }

    fun submitChangeEmail() {
        val normalizedEmail = normalizeEmail(changeEmailInput)
        val code = changeEmailCodeInput.trim()
        if (changeEmailProcessing) return
        if (normalizedEmail.isBlank()) {
            changeEmailErrorText = "Введите почту"
            return
        }
        if (!changeEmailCodeStep && normalizedEmail == normalizedAccountEmail) return
        if (changeEmailCodeStep && code.length < 6) {
            changeEmailErrorText = "Введите код с почты"
            return
        }

        changeEmailProcessing = true
        changeEmailErrorText = null
        scope.launch {
            val result = runCatching {
                authGateway.changeEmail(
                    email = normalizedEmail,
                    code = code.takeIf { changeEmailCodeStep }
                )
            }.getOrNull()

            when {
                result?.requiresEmailCode == true -> {
                    changeEmailCodeStep = true
                    changeEmailCodeInput = ""
                    changeEmailProcessing = false
                    Toast.makeText(
                        context,
                        result.message ?: "Код отправлен на текущую почту",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                result?.isSuccess == true -> {
                    onAccountEmailUpdated(normalizedEmail)
                    closeChangeEmailModal()
                    Toast.makeText(
                        context,
                        result.message ?: "Почта изменена",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    changeEmailErrorText = result?.message ?: "Не удалось изменить почту"
                    changeEmailProcessing = false
                }
            }
        }
    }

    suspend fun reloadProfile(): Boolean {
        val username = normalizeUsername(targetUsername) ?: return false
        val result = runCatching { profileGateway.loadProfile(username) }.getOrNull()
        if (result?.isSuccess == true) {
            profile = result.profile
            notifyChannelUpdated()
            return true
        }
        return false
    }

    fun reportActionResult(success: Boolean, message: String?, fallbackError: String) {
        if (success) return
        Toast.makeText(context, message ?: fallbackError, Toast.LENGTH_SHORT).show()
    }

    fun readImageBytes(uri: Uri): ByteArray? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    fun decodeImage(bytes: ByteArray): ImageBitmap? {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return bitmap.asImageBitmap()
    }

    fun uploadAvatar(uri: Uri) {
        if (avatarUploading) return
        val bytes = readImageBytes(uri)
        if (bytes == null) {
            Toast.makeText(context, "Не удалось прочитать изображение", Toast.LENGTH_SHORT).show()
            return
        }
        val previousAvatar = shownAvatar
        previewAvatar = decodeImage(bytes)
        avatarHiddenLocally = false
        awaitAvatarRemoteRefresh = false
        avatarUploading = true
        scope.launch {
            val result = runCatching {
                if (isChannelMode) {
                    profileGateway.uploadChannelAvatar(channelId!!, bytes)
                } else {
                    profileGateway.uploadProfileAvatar(bytes)
                }
            }.getOrNull()
            val success = result?.isSuccess == true
            reportActionResult(success, result?.message, "Не удалось обновить аватар")
            if (success) {
                homeGateway.evictImage(previousAvatar)
                val reloaded = reloadProfile()
                if (reloaded) {
                    homeGateway.evictImage(profile?.avatar)
                    avatarRefreshToken += 1
                    awaitAvatarRemoteRefresh = true
                }
            } else {
                previewAvatar = null
                awaitAvatarRemoteRefresh = false
            }
            avatarUploading = false
        }
    }

    fun uploadCover(uri: Uri) {
        if (coverUploading) return
        val bytes = readImageBytes(uri)
        if (bytes == null) {
            Toast.makeText(context, "Не удалось прочитать изображение", Toast.LENGTH_SHORT).show()
            return
        }
        val previousCover = profile?.cover
        previewCover = decodeImage(bytes)
        awaitCoverRemoteRefresh = false
        coverUploading = true
        scope.launch {
            val result = runCatching {
                if (isChannelMode) {
                    profileGateway.uploadChannelCover(channelId!!, bytes)
                } else {
                    profileGateway.uploadProfileCover(bytes)
                }
            }.getOrNull()
            val success = result?.isSuccess == true
            reportActionResult(success, result?.message, "Не удалось обновить обложку")
            if (success) {
                homeGateway.evictImage(previousCover)
                val reloaded = reloadProfile()
                if (reloaded) {
                    homeGateway.evictImage(profile?.cover)
                    coverRefreshToken += 1
                    awaitCoverRemoteRefresh = true
                }
            } else {
                previewCover = null
                awaitCoverRemoteRefresh = false
            }
            coverUploading = false
        }
    }

    fun applyProfileChanges() {
        if (profileSaving) return
        if (!nameChanged && !usernameChanged && !descriptionChanged) return
        profileSaving = true
        scope.launch {
            var hasError = false
            var hasSuccess = false

            if (usernameChanged && isChannelMode) {
                val normalizedUsername = normalizeUsername(editedUsername)
                if (normalizedUsername.isNullOrBlank()) {
                    hasError = true
                    reportActionResult(false, "Введите username канала", "Не удалось изменить username")
                } else {
                    val result = runCatching {
                        profileGateway.changeChannelUsername(channelId!!, normalizedUsername)
                    }.getOrNull()
                    val success = result?.isSuccess == true
                    if (success) {
                        hasSuccess = true
                        targetUsername = normalizedUsername
                    } else {
                        hasError = true
                        reportActionResult(false, result?.message, "Не удалось изменить username")
                    }
                }
            }

            if (nameChanged) {
                val result = runCatching {
                    if (isChannelMode) {
                        profileGateway.changeChannelName(channelId!!, editedName)
                    } else {
                        profileGateway.changeProfileName(editedName)
                    }
                }.getOrNull()
                val success = result?.isSuccess == true
                if (success) {
                    hasSuccess = true
                } else {
                    hasError = true
                    reportActionResult(false, result?.message, "Не удалось изменить имя")
                }
            }

            if (descriptionChanged) {
                val result = runCatching {
                    if (isChannelMode) {
                        profileGateway.changeChannelDescription(channelId!!, editedDescription)
                    } else {
                        profileGateway.changeProfileDescription(editedDescription)
                    }
                }.getOrNull()
                val success = result?.isSuccess == true
                if (success) {
                    hasSuccess = true
                } else {
                    hasError = true
                    reportActionResult(false, result?.message, "Не удалось изменить описание")
                }
            }

            if (hasSuccess) {
                val reloaded = reloadProfile()
                if (!reloaded) {
                    notifyChannelUpdated()
                }
            }

            if (!hasError && hasSuccess) {
                Toast.makeText(context, "Изменения применены", Toast.LENGTH_SHORT).show()
            } else if (hasError && hasSuccess) {
                Toast.makeText(context, "Часть изменений применена", Toast.LENGTH_SHORT).show()
            }
            profileSaving = false
        }
    }

    suspend fun saveProfileLink(
        linkId: Int?,
        title: String,
        link: String
    ): ActionResult {
        val trimmedTitle = title.trim()
        val trimmedLink = link.trim()
        val result = runCatching {
            if (linkId != null) {
                profileGateway.editProfileLink(linkId, trimmedTitle, trimmedLink)
            } else {
                profileGateway.addProfileLink(trimmedTitle, trimmedLink)
            }
        }.getOrNull() ?: ActionResult(
            status = "error",
            message = if (linkId != null) "Не удалось изменить ссылку" else "Не удалось добавить ссылку"
        )

        if (result.isSuccess) {
            val reloaded = reloadProfile()
            if (!reloaded && linkId != null) {
                profile = profile?.copy(
                    links = profile?.links.orEmpty().map { existing ->
                        if (existing.id == linkId) {
                            existing.copy(title = trimmedTitle, link = trimmedLink)
                        } else {
                            existing
                        }
                    }
                )
            }
        }

        return result
    }

    suspend fun removeProfileLink(linkId: Int): ActionResult {
        val result = runCatching {
            profileGateway.deleteProfileLink(linkId)
        }.getOrNull() ?: ActionResult(
            status = "error",
            message = "Не удалось удалить ссылку"
        )

        if (result.isSuccess) {
            profile = profile?.copy(
                links = profile?.links.orEmpty().filterNot { it.id == linkId }
            )
            reloadProfile()
        }

        return result
    }

    fun deleteProfileMedia(target: ProfileDeleteTarget) {
        if (avatarUploading || coverUploading) return
        pendingDeleteTarget = null
        val previousAvatar = shownAvatar
        val previousCover = profile?.cover

        if (target == ProfileDeleteTarget.Avatar) {
            avatarUploading = true
        } else {
            coverUploading = true
        }

        scope.launch {
            val result = runCatching {
                when (target) {
                    ProfileDeleteTarget.Avatar -> {
                        if (isChannelMode) {
                            profileGateway.deleteChannelAvatar(channelId!!)
                        } else {
                            profileGateway.deleteProfileAvatar()
                        }
                    }

                    ProfileDeleteTarget.Cover -> {
                        if (isChannelMode) {
                            profileGateway.deleteChannelCover(channelId!!)
                        } else {
                            profileGateway.deleteProfileCover()
                        }
                    }
                }
            }.getOrNull()
            val success = result?.isSuccess == true
            reportActionResult(success, result?.message, "Не удалось удалить изображение")

            if (success) {
                if (target == ProfileDeleteTarget.Avatar) {
                    homeGateway.evictImage(previousAvatar)
                    awaitAvatarRemoteRefresh = false
                    previewAvatar = null
                    avatarHiddenLocally = true
                    avatarRefreshToken += 1
                    profile = profile?.copy(avatar = null)
                    notifyChannelUpdated()
                } else {
                    homeGateway.evictImage(previousCover)
                    awaitCoverRemoteRefresh = false
                    previewCover = null
                    coverRefreshToken += 1
                    profile = profile?.copy(cover = null)
                }
                reloadProfile()
            }

            if (target == ProfileDeleteTarget.Avatar) {
                avatarUploading = false
            } else {
                coverUploading = false
            }
        }
    }

    fun startDeleteAccountConfirmation() {
        deleteAccountErrorText = null
        if (normalizedAccountEmail.isNullOrBlank()) {
            deleteAccountErrorText = "Не удалось получить почту аккаунта. Перезайдите в аккаунт и попробуйте снова."
            return
        }
        deleteAccountStep = DeleteAccountStep.ConfirmEmail
    }

    fun requestDeleteAccount(password: String) {
        if (deleteAccountProcessing) return
        deleteAccountProcessing = true
        deleteAccountErrorText = null

        scope.launch {
            val result = runCatching {
                authGateway.requestDeleteAccount(
                    password = password,
                    deletePosts = deletePostsWithAccount
                )
            }.getOrNull()

            when {
                result == null -> {
                    deleteAccountErrorText = "Не удалось удалить аккаунт"
                }

                result.requiresEmailCode -> {
                    deleteAccountStep = DeleteAccountStep.ConfirmCode
                    deleteAccountPasswordInput = ""
                    deleteAccountCodeInput = ""
                    Toast.makeText(
                        context,
                        result.message ?: "Код отправлен на почту",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                result.isSuccess -> {
                    closeDeleteAccountModal()
                    Toast.makeText(
                        context,
                        result.message ?: "Аккаунт успешно удалён",
                        Toast.LENGTH_SHORT
                    ).show()
                    onLogout()
                }

                else -> {
                    deleteAccountErrorText = result.message ?: "Не удалось удалить аккаунт"
                }
            }

            deleteAccountProcessing = false
        }
    }

    fun confirmDeleteAccount(code: String) {
        if (deleteAccountProcessing) return
        deleteAccountProcessing = true
        deleteAccountErrorText = null

        scope.launch {
            val result = runCatching {
                authGateway.confirmDeleteAccount(
                    code = code,
                    deletePosts = deletePostsWithAccount
                )
            }.getOrNull()

            if (result?.isSuccess == true) {
                closeDeleteAccountModal()
                Toast.makeText(
                    context,
                    result.message ?: "Аккаунт успешно удалён",
                    Toast.LENGTH_SHORT
                ).show()
                onLogout()
            } else {
                deleteAccountErrorText = result?.message ?: "Не удалось подтвердить удаление"
            }

            deleteAccountProcessing = false
        }
    }

    fun handleDeleteAccountAction() {
        when (deleteAccountStep) {
            null -> startDeleteAccountConfirmation()

            DeleteAccountStep.ConfirmEmail -> {
                val enteredEmail = deleteAccountEmailInput.trim().lowercase(Locale.ROOT)
                if (enteredEmail.isBlank()) {
                    deleteAccountErrorText = "Введите почту аккаунта"
                    return
                }
                if (enteredEmail != normalizedAccountEmail) {
                    deleteAccountErrorText = "Введённая почта не совпадает с почтой аккаунта"
                    return
                }

                deleteAccountErrorText = null
                deleteAccountStep = DeleteAccountStep.ConfirmPassword
            }

            DeleteAccountStep.ConfirmPassword -> {
                val password = deleteAccountPasswordInput
                if (password.isBlank()) {
                    deleteAccountErrorText = "Введите пароль"
                    return
                }
                requestDeleteAccount(password)
            }

            DeleteAccountStep.ConfirmCode -> {
                val code = deleteAccountCodeInput.trim()
                if (code.isBlank()) {
                    deleteAccountErrorText = "Введите код из письма"
                    return
                }
                confirmDeleteAccount(code)
            }
        }
    }

    fun submitChangePassword() {
        val oldPassword = changePasswordOldInput
        val newPassword = changePasswordNewInput
        val code = changePasswordCodeInput.trim().takeIf { changePasswordCodeStep }

        if (oldPassword.isBlank() || newPassword.isBlank()) {
            changePasswordErrorText = "Введите старый и новый пароль"
            return
        }

        if (changePasswordCodeStep && code.isNullOrBlank()) {
            changePasswordErrorText = "Введите код из письма"
            return
        }

        if (changePasswordProcessing) return
        changePasswordProcessing = true
        changePasswordErrorText = null

        scope.launch {
            val result = runCatching {
                authGateway.changePassword(
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    code = code
                )
            }.getOrNull()

            when {
                result == null -> {
                    changePasswordErrorText = "Не удалось сменить пароль"
                }

                result.requiresEmailCode -> {
                    changePasswordCodeStep = true
                    changePasswordCodeInput = ""
                    Toast.makeText(
                        context,
                        result.message ?: "Код отправлен на почту",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                result.isSuccess -> {
                    closeChangePasswordModal()
                    Toast.makeText(
                        context,
                        result.message ?: "Пароль изменён",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    changePasswordErrorText = result.message ?: "Не удалось сменить пароль"
                }
            }

            changePasswordProcessing = false
        }
    }

    val pickAvatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadAvatar(uri)
        }
    }

    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadCover(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ElementUiPalette.Block.copy(alpha = 0.72f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Назад",
                        tint = ElementUiPalette.TextPrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { rotationZ = 180f }
                    )
                }

                Text(
                    text = if (isChannelMode) "Редактирование канала" else "Редактирование профиля",
                    color = ElementUiPalette.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        SettingsEditProfile(
            title = if (onBack != null) "" else if (isChannelMode) "Редактирование канала" else "Редактирование профиля",
            name = editedName,
            onNameChange = { editedName = it },
            username = editedUsername.takeIf { isChannelMode },
            onUsernameChange = { editedUsername = normalizeUsername(it).orEmpty() },
            description = editedDescription,
            onDescriptionChange = { editedDescription = it },
            avatar = shownAvatar,
            cover = profile?.cover,
            homeGateway = homeGateway,
            avatarFallbackName = shownName,
            avatarPreview = previewAvatar,
            coverPreview = previewCover,
            avatarRefreshToken = avatarRefreshToken,
            coverRefreshToken = coverRefreshToken,
            awaitAvatarRemoteRefresh = awaitAvatarRemoteRefresh,
            awaitCoverRemoteRefresh = awaitCoverRemoteRefresh,
            isAvatarUploading = avatarUploading,
            isCoverUploading = coverUploading,
            onChangeAvatar = { pickAvatarLauncher.launch("image/*") },
            onDeleteAvatar = { pendingDeleteTarget = ProfileDeleteTarget.Avatar },
            onChangeCover = { pickCoverLauncher.launch("image/*") },
            onDeleteCover = { pendingDeleteTarget = ProfileDeleteTarget.Cover },
            isNameChanged = nameChanged,
            isUsernameChanged = usernameChanged,
            isDescriptionChanged = descriptionChanged,
            profileSaving = profileSaving,
            onAvatarRemoteBitmapReady = {
                previewAvatar = null
                awaitAvatarRemoteRefresh = false
                avatarHiddenLocally = false
            },
            onCoverRemoteBitmapReady = {
                previewCover = null
                awaitCoverRemoteRefresh = false
            },
            onApplyProfileChanges = ::applyProfileChanges,
            onResetName = { editedName = baseName },
            onResetUsername = { editedUsername = baseUsername },
            onResetDescription = { editedDescription = baseDescription },
            showLinksSection = !isChannelMode,
            links = profile?.links.orEmpty(),
            onAddLinkClick = {
                linkEditorRequest = SettingsLinkEditorRequest()
            },
            onEditLinkClick = { link ->
                linkEditorRequest = SettingsLinkEditorRequest(link = link)
            }
        )

        if (!isChannelMode) {
            if (AccountButtons.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SettingsPartitionTitle("Аккаунт")
                    SettingsMenuGroup(
                        entries = AccountButtons,
                        onClick = { entry ->
                            when (entry.type) {
                                "change_username" -> openChangeUsernameModal()
                                else -> showSoon(entry.label)
                            }
                        }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsPartitionTitle("Конфиденциальность")
                SettingsMenuGroup(
                    entries = ConfidentialityButtons,
                    onClick = { entry ->
                        when (entry.type) {
                            "sessions" -> sessionsModalOpen = true
                            "change_email" -> openChangeEmailModal()
                            "change_password" -> {
                                resetChangePasswordFlow()
                                changePasswordModalOpen = true
                            }
                            "storage" -> storageModalOpen = true
                            else -> showSoon(entry.label)
                        }
                    }
                )
            }
        }
    }

    if (OtherButtons.isNotEmpty()) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SettingsPartitionTitle("Прочее")
        SettingsMenuGroup(
            entries = OtherButtons,
            onClick = { entry ->
                when (entry.type) {
                    "elementum" -> {
                        elementumSettingsModalOpen = true
                    }

                    "advanced" -> {
                        advancedSettingsModalOpen = true
                    }

                    "delete_account" -> {
                        resetDeleteAccountFlow(resetPostsToggle = true)
                        deleteAccountModalOpen = true
                    }

                    else -> showSoon(entry.label)
                }
            }
        )
    }
}
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                SettingsPartitionTitle("Тип ленты")
                SettingsPostsType(
                    selected = defaultFeed,
                    onSelect = onDefaultFeedChanged
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                SettingsPartitionTitle("Смена темы")
                UIKit.SegmentTabs(
                    tabs = listOf("Светлая", "Темная", "AMOLED"),
                    selectedIndex = when (themeMode) {
                        AppThemeMode.Dark -> 1
                        AppThemeMode.Amoled -> 2
                        AppThemeMode.System,
                        AppThemeMode.Light -> 0
                    },
                    onSelect = { index ->
                        onThemeChanged(
                            when (index) {
                                1 -> AppThemeMode.Dark
                                2 -> AppThemeMode.Amoled
                                else -> AppThemeMode.Light
                            }
                        )
                    }
                )
            }

            SettingsBuildFooter()
        }

        Spacer(modifier = Modifier.height(74.dp))
    }

    linkEditorRequest?.takeIf { !isChannelMode }?.let { request ->
        SettingsLinkEditorModal(
            initialLink = request.link,
            accountAvatar = shownAvatar,
            avatarFallbackName = shownName,
            homeGateway = homeGateway,
            onClose = { linkEditorRequest = null },
            onSave = { linkId, title, link ->
                saveProfileLink(
                    linkId = linkId,
                    title = title,
                    link = link
                )
            },
            onDelete = { linkId ->
                removeProfileLink(linkId)
            }
        )
    }

    if (changeUsernameModalOpen && !isChannelMode) {
        SettingsChangeUsernameModal(
            username = changeUsernameInput,
            onUsernameChange = {
                changeUsernameInput = normalizeUsername(it).orEmpty()
                changeUsernameErrorText = null
            },
            currentUsername = normalizedAccountUsername,
            processing = changeUsernameProcessing,
            errorText = changeUsernameErrorText,
            onClose = ::closeChangeUsernameModal,
            onAction = ::submitChangeUsername
        )
    }

    if (changeEmailModalOpen && !isChannelMode) {
        SettingsChangeEmailModal(
            email = changeEmailInput,
            onEmailChange = {
                changeEmailInput = it
                changeEmailErrorText = null
            },
            currentEmail = normalizedAccountEmail,
            code = changeEmailCodeInput,
            onCodeChange = {
                changeEmailCodeInput = it
                changeEmailErrorText = null
            },
            codeStep = changeEmailCodeStep,
            processing = changeEmailProcessing,
            errorText = changeEmailErrorText,
            onClose = ::closeChangeEmailModal,
            onAction = ::submitChangeEmail
        )
    }

    if (sessionsModalOpen) {
        SettingsSessionsModal(
            authGateway = authGateway,
            onClose = { sessionsModalOpen = false }
        )
    }

    if (storageModalOpen) {
        SettingsStorageModal(
            homeGateway = homeGateway,
            onClose = { storageModalOpen = false }
        )
    }

    if (changePasswordModalOpen) {
        SettingsChangePasswordModal(
            oldPassword = changePasswordOldInput,
            onOldPasswordChange = {
                changePasswordOldInput = it
                changePasswordErrorText = null
            },
            newPassword = changePasswordNewInput,
            onNewPasswordChange = {
                changePasswordNewInput = it
                changePasswordErrorText = null
            },
            code = changePasswordCodeInput,
            onCodeChange = {
                changePasswordCodeInput = it
                changePasswordErrorText = null
            },
            codeStep = changePasswordCodeStep,
            processing = changePasswordProcessing,
            errorText = changePasswordErrorText,
            showOldPassword = showOldPassword,
            onShowOldPasswordChange = { showOldPassword = it },
            showNewPassword = showNewPassword,
            onShowNewPasswordChange = { showNewPassword = it },
            onClose = ::closeChangePasswordModal,
            onAction = ::submitChangePassword
        )
    }
    if (elementumSettingsModalOpen) {
        SettingsElementumModal(
            onClose = { elementumSettingsModalOpen = false }
         )
    }
    if (advancedSettingsModalOpen) {
        SettingsAdvancedModal(
            notificationsToastEnabled = notificationsToastEnabled,
            autoVideoDownloadEnabled = autoVideoDownloadEnabled,
            videoAutoplayEnabled = videoAutoplayEnabled,
            transparencyMode = transparencyMode,
            onNotificationsToastChanged = onNotificationsToastChanged,
            onAutoVideoDownloadChanged = onAutoVideoDownloadChanged,
            onVideoAutoplayChanged = onVideoAutoplayChanged,
            onTransparencyModeChanged = onTransparencyModeChanged,
            onClose = { advancedSettingsModalOpen = false }
        )
    }

    if (deleteAccountModalOpen) {
        SettingsDeleteAccountModal(
            deletePostsWithAccount = deletePostsWithAccount,
            onDeletePostsWithAccountChange = { deletePostsWithAccount = it },
            currentStep = deleteAccountStep,
            accountEmail = normalizedAccountEmail,
            emailInput = deleteAccountEmailInput,
            onEmailInputChange = {
                deleteAccountEmailInput = it
                deleteAccountErrorText = null
            },
            passwordInput = deleteAccountPasswordInput,
            onPasswordInputChange = {
                deleteAccountPasswordInput = it
                deleteAccountErrorText = null
            },
            codeInput = deleteAccountCodeInput,
            onCodeInputChange = {
                deleteAccountCodeInput = it
                deleteAccountErrorText = null
            },
            errorText = deleteAccountErrorText,
            processing = deleteAccountProcessing,
            onClose = ::closeDeleteAccountModal,
            onAction = ::handleDeleteAccountAction
        )
    }

    if (pendingDeleteTarget != null) {
        val isAvatarTarget = pendingDeleteTarget == ProfileDeleteTarget.Avatar
        val title = if (isAvatarTarget) "Удалить аватар?" else "Удалить обложку?"
        val message = if (isAvatarTarget) {
            "Вы уверены что хотите удалить аватар?"
        } else {
            "Вы уверены что хотите удалить обложку?"
        }

        AlertDialog(
            onDismissRequest = { pendingDeleteTarget = null },
            title = { SettingsModalAlertTitle(title) },
            text = { SettingsModalAlertMessage(message) },
            properties = DialogProperties(decorFitsSystemWindows = false),
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingDeleteTarget ?: return@TextButton
                        deleteProfileMedia(target)
                    }
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTarget = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun SettingsChangeUsernameModal(
    username: String,
    onUsernameChange: (String) -> Unit,
    currentUsername: String?,
    processing: Boolean,
    errorText: String?,
    onClose: () -> Unit,
    onAction: () -> Unit
) {
    val normalizedUsername = normalizeUsername(username)
    UIKit.RoutedModal(
        title = "Уникальное имя",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 2.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_modal_change_username),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(width = 164.dp, height = 108.dp)
                    )
                }

                Text(
                    text = "Сменить имя можно сколько угодно раз, но если его займёт кто-то другой вернуть уже не получится.",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = SettingsModalSecondaryTextSize,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UIKit.Input(
                        value = username,
                        onValueChange = { onUsernameChange(it.take(64)) },
                        placeholder = "@введите_текст",
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = ElementUiPalette.Block,
                        borderWidth = 1.dp,
                        borderColor = ElementUiPalette.scaledBorder(0.9f)
                    )

                    if (!errorText.isNullOrBlank()) {
                        Text(
                            text = errorText,
                            color = ElementUiPalette.Error,
                            fontSize = SettingsModalSecondaryTextSize
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    UIKit.Button(
                        title = "Сменить",
                        onClick = onAction,
                        enabled = !processing &&
                            !normalizedUsername.isNullOrBlank() &&
                            normalizedUsername != currentUsername,
                        loading = processing,
                        textFontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsChangeEmailModal(
    email: String,
    onEmailChange: (String) -> Unit,
    currentEmail: String?,
    code: String,
    onCodeChange: (String) -> Unit,
    codeStep: Boolean,
    processing: Boolean,
    errorText: String?,
    onClose: () -> Unit,
    onAction: () -> Unit
) {
    val normalizedEmail = email.trim().lowercase(Locale.ROOT)
    UIKit.RoutedModal(
        title = "Сменить почту",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 2.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_modal_change_email),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(width = 164.dp, height = 108.dp)
                    )
                }

                if (!currentEmail.isNullOrBlank()) {
                    Text(
                        text = "Текущая: $currentEmail",
                        color = ElementUiPalette.TextSecondary,
                        fontSize = SettingsModalSecondaryTextSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        SettingsInputLabel("Почта")
                        UIKit.Input(
                            value = email,
                            onValueChange = { onEmailChange(it.take(150)) },
                            placeholder = "Введите почту",
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = ElementUiPalette.Block,
                            borderWidth = 1.dp,
                            borderColor = ElementUiPalette.scaledBorder(0.9f)
                        )
                    }

                    if (codeStep) {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            SettingsInputLabel("Код подтверждения")
                            UIKit.Input(
                                value = code,
                                onValueChange = { onCodeChange(it.take(7)) },
                                placeholder = "Код с почты",
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = ElementUiPalette.Block,
                                borderWidth = 1.dp,
                                borderColor = ElementUiPalette.scaledBorder(0.9f)
                            )
                        }
                    }

                    if (!errorText.isNullOrBlank()) {
                        Text(
                            text = errorText,
                            color = ElementUiPalette.Error,
                            fontSize = SettingsModalSecondaryTextSize
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    UIKit.Button(
                        title = if (codeStep) "Подтвердить" else "Сменить",
                        onClick = onAction,
                        enabled = if (codeStep) {
                            !processing && normalizedEmail.isNotBlank() && code.trim().length >= 6
                        } else {
                            !processing && normalizedEmail.isNotBlank() && normalizedEmail != currentEmail
                        },
                        loading = processing,
                        textFontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsModalHeroIcon(
    @DrawableRes iconRes: Int,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(74.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.24f),
                        tint.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = tint.copy(alpha = 0.16f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun SettingsElementumModal(
    onClose: () -> Unit
) {
    UIKit.RoutedModal(
        title = "Настройки Elementum",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp, bottom = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsPartitionTitle("Основное")
                UIKit.Block(
                    modifier = Modifier.fillMaxWidth(),
                    showShadow = false,
                    contentPadding = 12.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Здесь будут настройки Elementum",
                            color = ElementUiPalette.TextSecondary,
                            fontSize = SettingsModalSecondaryTextSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsAdvancedModal(
    notificationsToastEnabled: Boolean,
    autoVideoDownloadEnabled: Boolean,
    videoAutoplayEnabled: Boolean,
    transparencyMode: TransparencyMode,
    onNotificationsToastChanged: (Boolean) -> Unit,
    onAutoVideoDownloadChanged: (Boolean) -> Unit,
    onVideoAutoplayChanged: (Boolean) -> Unit,
    onTransparencyModeChanged: (TransparencyMode) -> Unit,
    onClose: () -> Unit
) {
    UIKit.RoutedModal(
        title = "Расширенные настройки",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp, bottom = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsPartitionTitle("Видео")
                UIKit.Block(
                    modifier = Modifier.fillMaxWidth(),
                    showShadow = false,
                    contentPadding = 12.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsToggleRow(
                            title = "Автовоспроизведение видео",
                            checked = videoAutoplayEnabled,
                            onCheckedChange = onVideoAutoplayChanged
                        )
                        SettingsToggleRow(
                            title = "Автозагрузка видео",
                            checked = autoVideoDownloadEnabled,
                            onCheckedChange = onAutoVideoDownloadChanged
                        )
                        SettingsTransparencyRow(
                            title = "Прозрачность элементов",
                            currentMode = transparencyMode,
                            onModeChanged = onTransparencyModeChanged
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsPartitionTitle("Уведомления")
                UIKit.Block(
                    modifier = Modifier.fillMaxWidth(),
                    showShadow = false,
                    contentPadding = 12.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsToggleRow(
                            title = "Всплывающие уведомления",
                            checked = notificationsToastEnabled,
                            onCheckedChange = onNotificationsToastChanged
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsChangePasswordModal(
    oldPassword: String,
    onOldPasswordChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    codeStep: Boolean,
    processing: Boolean,
    errorText: String?,
    showOldPassword: Boolean,
    onShowOldPasswordChange: (Boolean) -> Unit,
    showNewPassword: Boolean,
    onShowNewPasswordChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onAction: () -> Unit
) {
    UIKit.RoutedModal(
        title = "Сменить пароль",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 2.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SessionSvgImage(
                        svg = SettingsSecuritySvg.ChangePassword,
                        width = 164.dp,
                        height = 108.dp
                    )
                }

                Text(
                    text = "Запомните или запишите пароль, если вы его забудете, вы не сможете войти в аккаунт.",
                    color = ElementUiPalette.TextSecondary,
                    fontSize = SettingsModalSecondaryTextSize,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                )

                UIKit.Block(
                    modifier = Modifier.fillMaxWidth(),
                    showShadow = false,
                    contentPadding = 12.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsPasswordInput(
                            value = oldPassword,
                            onValueChange = { onOldPasswordChange(it.take(100)) },
                            placeholder = "Старый пароль",
                            visible = showOldPassword,
                            onVisibilityToggle = { onShowOldPasswordChange(!showOldPassword) }
                        )
                        SettingsPasswordInput(
                            value = newPassword,
                            onValueChange = { onNewPasswordChange(it.take(100)) },
                            placeholder = "Новый пароль",
                            visible = showNewPassword,
                            onVisibilityToggle = { onShowNewPasswordChange(!showNewPassword) }
                        )
                        if (codeStep) {
                            UIKit.Input(
                                value = code,
                                onValueChange = { onCodeChange(it.take(7)) },
                                placeholder = "Код с почты",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (!errorText.isNullOrBlank()) {
                            Text(
                                text = errorText,
                                color = ElementUiPalette.Error,
                                fontSize = SettingsModalSecondaryTextSize
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    UIKit.Button(
                        title = if (codeStep) "Подтвердить" else "Сменить",
                        onClick = onAction,
                        enabled = if (codeStep) {
                            code.trim().length >= 6 && !processing
                        } else {
                            oldPassword.isNotBlank() && newPassword.isNotBlank() && !processing
                        },
                        loading = processing,
                        textFontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsDeleteAccountModal(
    deletePostsWithAccount: Boolean,
    onDeletePostsWithAccountChange: (Boolean) -> Unit,
    currentStep: DeleteAccountStep?,
    accountEmail: String?,
    emailInput: String,
    onEmailInputChange: (String) -> Unit,
    passwordInput: String,
    onPasswordInputChange: (String) -> Unit,
    codeInput: String,
    onCodeInputChange: (String) -> Unit,
    errorText: String?,
    processing: Boolean,
    onClose: () -> Unit,
    onAction: () -> Unit
) {
    UIKit.RoutedModal(
        title = "Удалить аккаунт",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 2.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UIKit.Block(
                    modifier = Modifier.fillMaxWidth(),
                    showShadow = false,
                    contentPadding = 12.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Удаление аккаунта",
                            color = ElementUiPalette.TextPrimary,
                            fontSize = SettingsModalPrimaryTextSize,
                            fontWeight = FontWeight.Medium
                        )
                        SettingsDeleteAccountParagraph(
                            "При удалении аккаунта, все ваши личные данные (имя аккаунта, почта, хэш пароля, сессии, чаты) будут удалены с базы данных Element'а в течении 14 дней."
                        )
                        SettingsDeleteAccountParagraph(
                            "Если вы измените решение можно восстановить аккаунт в течении этих 14 дней, просто войдите в аккаунт до окончания этого срока, и удаление будет отменено. После 14 дней, аккаунт будет удалён без возможности восстановления."
                        )
                        SettingsDeleteAccountParagraph(
                            "Ваши посты после удаления останутся не тронутыми, ваш аккаунт просто потеряет все личные данные, если вы хотите вы можете отдельно поставить отметку при удалении аккаунта и ваши посты будут удалены вместе с аккаунтом, а точнее перемещены в корзину, соответственно вы сможете восстановить их вместе с аккаунтом в течении 14 дней, после чего они будут удалены без возможности восстановления."
                        )
                        SettingsDeleteAccountParagraph(
                            "Вы так же можете скачать архив с вашими данными во вкладке «Продвинутые настройки»"
                        )
                    }
                }

                UIKit.Block(
                    modifier = Modifier.fillMaxWidth(),
                    showShadow = false,
                    contentPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Удалить все мои посты вместе с аккаунтом",
                            color = ElementUiPalette.TextPrimary,
                            fontSize = SettingsModalPrimaryTextSize,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = deletePostsWithAccount,
                            onCheckedChange = onDeletePostsWithAccountChange
                        )
                    }
                }

                if (currentStep != null) {
                    UIKit.Block(
                        modifier = Modifier.fillMaxWidth(),
                        showShadow = false,
                        contentPadding = 12.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            val stepTitle = when (currentStep) {
                                DeleteAccountStep.ConfirmEmail -> "Подтвердите почту"
                                DeleteAccountStep.ConfirmPassword -> "Подтвердите пароль"
                                DeleteAccountStep.ConfirmCode -> "Введите код из письма"
                            }
                            val stepDescription = when (currentStep) {
                                DeleteAccountStep.ConfirmEmail ->
                                    "Для подтверждения удаления аккаунта введите вашу почту"

                                DeleteAccountStep.ConfirmPassword ->
                                    "Для подтверждения удаления аккаунта введите ваш пароль"

                                DeleteAccountStep.ConfirmCode ->
                                    "Введите код, отправленный на вашу почту"
                            }
                            val placeholder = when (currentStep) {
                                DeleteAccountStep.ConfirmEmail -> "Почта аккаунта"
                                DeleteAccountStep.ConfirmPassword -> "Пароль"
                                DeleteAccountStep.ConfirmCode -> "Код подтверждения"
                            }

                            Text(
                                text = stepTitle,
                                color = ElementUiPalette.TextPrimary,
                                fontSize = SettingsModalPrimaryTextSize,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stepDescription,
                                color = ElementUiPalette.TextSecondary,
                                fontSize = SettingsModalSecondaryTextSize,
                                lineHeight = 17.sp
                            )
                            if (currentStep == DeleteAccountStep.ConfirmEmail && !accountEmail.isNullOrBlank()) {
                                Text(
                                    text = "Почта аккаунта: $accountEmail",
                                    color = ElementUiPalette.TextSecondary,
                                    fontSize = SettingsModalSupportTextSize
                                )
                            }
                            UIKit.Input(
                                value = when (currentStep) {
                                    DeleteAccountStep.ConfirmEmail -> emailInput
                                    DeleteAccountStep.ConfirmPassword -> passwordInput
                                    DeleteAccountStep.ConfirmCode -> codeInput
                                },
                                onValueChange = { value ->
                                    when (currentStep) {
                                        DeleteAccountStep.ConfirmEmail -> onEmailInputChange(value.take(150))
                                        DeleteAccountStep.ConfirmPassword -> onPasswordInputChange(value.take(100))
                                        DeleteAccountStep.ConfirmCode -> onCodeInputChange(value.take(12))
                                    }
                                },
                                placeholder = placeholder,
                                visualTransformation = if (currentStep == DeleteAccountStep.ConfirmPassword) {
                                    PasswordVisualTransformation()
                                } else {
                                    androidx.compose.ui.text.input.VisualTransformation.None
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (!errorText.isNullOrBlank()) {
                    UIKit.Block(
                        modifier = Modifier.fillMaxWidth(),
                        showShadow = false
                    ) {
                        Text(
                            text = errorText,
                            color = ElementUiPalette.Error,
                            fontSize = SettingsModalSecondaryTextSize
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            ) {
                UIKit.Button(
                    title = when (currentStep) {
                        null -> "Принять и удалить"
                        DeleteAccountStep.ConfirmEmail -> "Подтвердить почту"
                        DeleteAccountStep.ConfirmPassword -> "Принять и удалить"
                        DeleteAccountStep.ConfirmCode -> "Подтвердить код"
                    },
                    onClick = onAction,
                    enabled = !processing,
                    loading = processing,
                    textFontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SettingsDeleteAccountParagraph(text: String) {
    Text(
        text = text,
        color = ElementUiPalette.TextSecondary,
        fontSize = SettingsModalSecondaryTextSize,
        lineHeight = 18.sp
    )
}

@Composable
private fun SettingsPasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    UIKit.Input(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        visualTransformation = if (visible) {
            androidx.compose.ui.text.input.VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            UIKit.PlainIconButton(
                icon = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                onClick = onVisibilityToggle
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private data class StorageChartSegment(
    val color: Color,
    val sweepAngle: Float
)

private fun normalizeStorageBytes(bytes: Long): Long {
    return if (bytes in 1..1023) 1024L else bytes
}

private fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0L) return "0 Б"
    val units = listOf("Б", "КБ", "МБ", "ГБ", "ТБ")
    val index = (ln(bytes.toDouble()) / ln(1024.0))
        .toInt()
        .coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(index.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[index])
}

private fun buildStorageChartSegments(categories: List<StorageUiCategory>): List<StorageChartSegment> {
    if (categories.isEmpty()) return emptyList()
    if (categories.size == 1) return listOf(StorageChartSegment(categories.first().color, 360f))

    val total = categories.sumOf { it.sizeBytes }.coerceAtLeast(1L).toDouble()
    val minPercent = 5.0
    val adjusted = categories.map { category ->
        val raw = if (category.sizeBytes <= 0L) 0.0 else category.sizeBytes / total * 100.0
        if (raw in 0.0..minPercent && category.sizeBytes > 0L) minPercent else raw
    }.toMutableList()

    val sumAdjusted = adjusted.sum()
    if (sumAdjusted > 100.0 && sumAdjusted > 0.0) {
        val ratio = 100.0 / sumAdjusted
        for (index in adjusted.indices) {
            adjusted[index] *= ratio
        }
    }

    return categories.mapIndexed { index, category ->
        StorageChartSegment(
            color = category.color,
            sweepAngle = (adjusted[index] * 3.6).toFloat()
        )
    }
}

private fun categoriesCountWord(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "категория"
        mod10 in 2..4 && mod100 !in 12..14 -> "категории"
        else -> "категорий"
    }
}

private fun buildClearSelectedLabel(categories: List<StorageUiCategory>): String {
    val selected = categories.filter { it.selected }
    if (selected.isEmpty()) return "Очистить"
    if (selected.size == categories.size) return "Очистить хранилище"
    if (selected.size <= 2) {
        return "Очистить: ${selected.joinToString(", ") { it.title }}"
    }
    return "Очистить: ${selected.size} ${categoriesCountWord(selected.size)}"
}

@Composable
private fun SettingsStorageModal(
    homeGateway: HomeGateway,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf(StorageCategoriesTemplate) }
    var totalBytes by remember { mutableStateOf(0L) }
    var clearing by remember { mutableStateOf(false) }
    var pendingClear by remember { mutableStateOf<StorageClearDialogTarget?>(null) }
    var showSuccess by remember { mutableStateOf(false) }

    fun applyStats(stats: ImageDiskCache.StorageStats?) {
        val mapped = StorageCategoriesTemplate.map { template ->
            val raw = stats?.categoryBytes?.get(template.id.toCacheCategory()) ?: 0L
            template.copy(
                sizeBytes = normalizeStorageBytes(raw),
                selected = true
            )
        }
        categories = mapped
        totalBytes = mapped.sumOf { it.sizeBytes }
    }

    fun refreshStorage() {
        scope.launch {
            val stats = runCatching { homeGateway.loadStorageStats() }.getOrNull()
            applyStats(stats)
        }
    }

    fun toggleCategory(categoryId: StorageUiCategoryId) {
        val current = categories.find { it.id == categoryId } ?: return
        if (current.selected && categories.count { it.selected } == 1) return

        categories = categories.map { category ->
            if (category.id == categoryId) category.copy(selected = !category.selected) else category
        }
    }

    fun clearSelectedCategories(includeAll: Boolean) {
        if (clearing) return
        val targets = if (includeAll) {
            ImageDiskCache.StorageCategory.entries.toSet()
        } else {
            categories.filter { it.selected }.map { it.id.toCacheCategory() }.toSet()
        }
        if (targets.isEmpty()) return

        clearing = true
        scope.launch {
            runCatching {
                homeGateway.clearStorageCategories(targets)
            }
            val refreshed = runCatching { homeGateway.loadStorageStats() }.getOrNull()
            applyStats(refreshed)
            clearing = false
            showSuccess = true
            delay(1200)
            showSuccess = false
        }
    }

    LaunchedEffect(Unit) {
        refreshStorage()
    }

    val selectedBytes = categories.filter { it.selected }.sumOf { it.sizeBytes }
    val chartCategories = categories.filter { it.selected && it.sizeBytes > 0L }

    UIKit.RoutedModal(
        title = "Хранилище",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    StorageUsageChart(
                        categories = chartCategories,
                        selectedBytes = selectedBytes,
                        showSuccess = showSuccess
                    )

                    Text(
                        text = "Использование хранилища",
                        color = ElementUiPalette.TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = SettingsModalPrimaryTextSize
                    )
                    Text(
                        text = "Занято: ${formatStorageSize(totalBytes)}",
                        color = ElementUiPalette.TextLite,
                        fontSize = SettingsModalSecondaryTextSize
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Выберите что очистить",
                        color = ElementUiPalette.TextLite,
                        fontSize = SettingsModalSectionTitleTextSize,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 1.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        categories.forEach { category ->
                            StorageCategoryRow(
                                category = category,
                                onToggle = { toggleCategory(category.id) }
                            )
                        }
                    }
                }

                Text(
                    text = "Данные в облаке не удаляются.",
                    color = ElementUiPalette.TextLite,
                    fontSize = SettingsModalSupportTextSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UIKit.Button(
                    title = if (clearing) "Очистка..." else buildClearSelectedLabel(categories),
                    onClick = { pendingClear = StorageClearDialogTarget.Selected },
                    enabled = !clearing
                )
                UIKit.Button(
                    title = if (clearing) "Очистка..." else "Очистить весь кэш",
                    onClick = { pendingClear = StorageClearDialogTarget.All },
                    enabled = !clearing,
                    variant = ElementButtonVariant.Soft
                )
            }
        }
    }

    if (pendingClear != null) {
        val clearAll = pendingClear == StorageClearDialogTarget.All
        val selectedNames = categories
            .filter { it.selected }
            .joinToString(", ") { it.title }

        AlertDialog(
            onDismissRequest = { pendingClear = null },
            title = { SettingsModalAlertTitle("Вы уверены?") },
            text = {
                SettingsModalAlertMessage(
                    if (clearAll) {
                        "Очистить весь локальный кэш?"
                    } else {
                        "Очистить выбранные категории: $selectedNames?"
                    }
                )
            },
            properties = DialogProperties(decorFitsSystemWindows = false),
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingClear
                        pendingClear = null
                        if (target == StorageClearDialogTarget.All) {
                            clearSelectedCategories(includeAll = true)
                        } else {
                            clearSelectedCategories(includeAll = false)
                        }
                    }
                ) { Text("Очистить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingClear = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun StorageCategoryRow(
    category: StorageUiCategory,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.Block)
            .clickable(onClick = onToggle)
            .padding(start = 6.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(category.color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.title,
                color = ElementUiPalette.TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = SettingsModalPrimaryTextSize
            )
            Text(
                text = formatStorageSize(category.sizeBytes),
                color = ElementUiPalette.TextLite,
                fontSize = SettingsModalSecondaryTextSize
            )
        }

        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (category.selected) category.color else ElementUiPalette.TextLite,
                        shape = CircleShape
                    )
                    .background(if (category.selected) category.color else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (category.selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageUsageChart(
    categories: List<StorageUiCategory>,
    selectedBytes: Long,
    showSuccess: Boolean
) {
    Box(
        modifier = Modifier.size(136.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showSuccess) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF45A049)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerRadius = size.minDimension * 0.4f
            val outerDiameter = outerRadius * 2f
            val topLeft = Offset(center.x - outerRadius, center.y - outerRadius)
            val segments = buildStorageChartSegments(categories)

            if (segments.isEmpty()) {
                drawCircle(
                    color = Color(0xFF333333).copy(alpha = 0.3f),
                    radius = outerRadius,
                    center = center,
                    style = Stroke(
                        width = 1.3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                            0f
                        )
                    )
                )
            } else {
                var startAngle = 0f
                rotate(degrees = -90f, pivot = center) {
                    segments.forEach { segment ->
                        drawArc(
                            color = segment.color,
                            startAngle = startAngle,
                            sweepAngle = segment.sweepAngle,
                            useCenter = true,
                            topLeft = topLeft,
                            size = Size(outerDiameter, outerDiameter)
                        )
                        startAngle += segment.sweepAngle
                    }
                }
            }

            drawCircle(
                color = ElementUiPalette.Body,
                radius = outerRadius * 0.75f,
                center = center
            )
        }

        Text(
            text = formatStorageSize(selectedBytes),
            color = ElementUiPalette.TextPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SettingsEditProfile(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    username: String?,
    onUsernameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    avatar: PostImageAsset?,
    cover: PostImageAsset?,
    homeGateway: HomeGateway,
    avatarFallbackName: String,
    avatarPreview: ImageBitmap?,
    coverPreview: ImageBitmap?,
    avatarRefreshToken: Int,
    coverRefreshToken: Int,
    awaitAvatarRemoteRefresh: Boolean,
    awaitCoverRemoteRefresh: Boolean,
    isAvatarUploading: Boolean,
    isCoverUploading: Boolean,
    onChangeAvatar: () -> Unit,
    onDeleteAvatar: () -> Unit,
    onChangeCover: () -> Unit,
    onDeleteCover: () -> Unit,
    isNameChanged: Boolean,
    isUsernameChanged: Boolean,
    isDescriptionChanged: Boolean,
    profileSaving: Boolean,
    onAvatarRemoteBitmapReady: () -> Unit,
    onCoverRemoteBitmapReady: () -> Unit,
    onApplyProfileChanges: () -> Unit,
    onResetName: () -> Unit,
    onResetUsername: () -> Unit,
    onResetDescription: () -> Unit,
    showLinksSection: Boolean,
    links: List<ProfileLink>,
    onAddLinkClick: () -> Unit,
    onEditLinkClick: (ProfileLink) -> Unit
) {
    UIKit.Block(showShadow = false) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    color = ElementUiPalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.TopStart)
                ) {
                    SettingsCover(
                        asset = cover,
                        preview = coverPreview,
                        refreshToken = coverRefreshToken,
                        awaitRemoteRefresh = awaitCoverRemoteRefresh,
                        isUploading = isCoverUploading,
                        homeGateway = homeGateway,
                        onRemoteBitmapReady = onCoverRemoteBitmapReady,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )

                    SettingsChangeButtons(
                        onUpload = onChangeCover,
                        onDelete = onDeleteCover,
                        enabled = !isCoverUploading,
                        buttonWidth = 102.dp,
                        cornerRadius = 9.dp,
                        buttonHeight = 28.dp,
                        buttonSpacing = 3.dp,
                        buttonTextFontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .zIndex(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = 158.dp)
                        .padding(start = 8.dp)
                        .zIndex(3f)
                ) {
                    SettingsAvatar(
                        name = avatarFallbackName,
                        asset = avatar,
                        preview = avatarPreview,
                        refreshToken = avatarRefreshToken,
                        awaitRemoteRefresh = awaitAvatarRemoteRefresh,
                        isUploading = isAvatarUploading,
                        homeGateway = homeGateway,
                        onRemoteBitmapReady = onAvatarRemoteBitmapReady,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(86.dp)
                            .zIndex(2f)
                    )

                    SettingsChangeButtons(
                        onUpload = onChangeAvatar,
                        onDelete = onDeleteAvatar,
                        enabled = !isAvatarUploading,
                        buttonWidth = 122.dp,
                        cornerRadius = 10.dp,
                        buttonHeight = 31.dp,
                        buttonSpacing = 3.dp,
                        buttonTextFontSize = 12.sp,
                        flatLeftEdge = true,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = 50.dp, y = 8.dp)
                            .zIndex(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                SettingsInputLabel("Имя")
                UIKit.Input(
                    value = name,
                    onValueChange = { onNameChange(it.take(60)) },
                    placeholder = "Введите имя",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp)
                )
            }
            SettingsChangeQuestion(
                visible = isNameChanged,
                loading = profileSaving,
                onApply = onApplyProfileChanges,
                onCancel = onResetName
            )

            if (username != null) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    SettingsInputLabel("Username")
                    UIKit.Input(
                        value = username,
                        onValueChange = { onUsernameChange(it.take(60)) },
                        placeholder = "Введите username",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 5.dp)
                    )
                }
                SettingsChangeQuestion(
                    visible = isUsernameChanged,
                    loading = profileSaving,
                    onApply = onApplyProfileChanges,
                    onCancel = onResetUsername
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                SettingsInputLabel("Описание")
                UIKit.Input(
                    value = description,
                    onValueChange = { onDescriptionChange(it.take(1000)) },
                    placeholder = "Введите описание",
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp)
                        .heightIn(min = 80.dp)
                )
            }
            SettingsChangeQuestion(
                visible = isDescriptionChanged,
                loading = profileSaving,
                onApply = onApplyProfileChanges,
                onCancel = onResetDescription
            )

            if (showLinksSection) {
                SettingsLinksEditorSection(
                    links = links,
                    onAddClick = onAddLinkClick,
                    onEditClick = onEditLinkClick
                )
            }
        }
    }
}

@Composable
private fun SettingsLinksEditorSection(
    links: List<ProfileLink>,
    onAddClick: () -> Unit,
    onEditClick: (ProfileLink) -> Unit
) {
    val chipShape = RoundedCornerShape(12.dp)
    val orderedLinks = remember(links) {
        links.sortedByDescending { it.id ?: Int.MIN_VALUE }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SettingsInputLabel("Ссылки")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(chipShape)
                    .background(ElementUiPalette.Accent.copy(alpha = 0.12f))
                    .border(1.dp, ElementUiPalette.Accent.copy(alpha = 0.26f), chipShape)
                    .clickable(onClick = onAddClick)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = ElementUiPalette.Accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Добавить",
                    color = ElementUiPalette.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            orderedLinks.forEach { link ->
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .widthIn(max = 190.dp)
                        .clip(chipShape)
                        .background(ElementUiPalette.BlockSoft.copy(alpha = 0.94f))
                        .border(1.dp, ElementUiPalette.scaledBorder(0.85f), chipShape)
                        .clickable { onEditClick(link) }
                        .padding(horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    ProfileLinkIcon(link = link.link, size = 18.dp)
                    Text(
                        text = link.title,
                        color = ElementUiPalette.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (orderedLinks.isEmpty()) {
            Text(
                text = "Добавьте ссылки на свои соцсети и сайты",
                color = ElementUiPalette.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 11.dp)
            )
        }
    }
}

@Composable
private fun SettingsLinkEditorModal(
    initialLink: ProfileLink?,
    accountAvatar: PostImageAsset?,
    avatarFallbackName: String,
    homeGateway: HomeGateway,
    onClose: () -> Unit,
    onSave: suspend (linkId: Int?, title: String, link: String) -> ActionResult,
    onDelete: suspend (linkId: Int) -> ActionResult
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val linkId = initialLink?.id
    val isEditing = linkId != null
    var title by remember(initialLink?.id, initialLink?.title) {
        mutableStateOf(initialLink?.title.orEmpty())
    }
    var link by remember(initialLink?.id, initialLink?.link) {
        mutableStateOf(initialLink?.link.orEmpty())
    }
    var processing by remember(initialLink?.id) { mutableStateOf(false) }
    val canSubmit = title.trim().isNotEmpty() && link.trim().isNotEmpty()
    val previewLink = link.trim().ifBlank { "https://elemsocial.com" }

    UIKit.RoutedModal(
        title = if (isEditing) "Изменить ссылку" else "Добавить ссылку",
        onClose = onClose,
        contentHorizontalPadding = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(108.dp)) {
                    SettingsAvatar(
                        name = avatarFallbackName,
                        asset = accountAvatar,
                        preview = null,
                        refreshToken = 0,
                        awaitRemoteRefresh = false,
                        isUploading = false,
                        homeGateway = homeGateway,
                        onRemoteBitmapReady = {},
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(88.dp)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ElementUiPalette.Block)
                            .border(1.dp, ElementUiPalette.scaledBorder(0.92f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileLinkIcon(
                            link = previewLink,
                            size = 18.dp
                        )
                    }
                }
            }

            UIKit.Block(showShadow = false, contentPadding = 8.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        SettingsInputLabel("Имя ссылки")
                        UIKit.Input(
                            value = title,
                            onValueChange = { title = it.take(50) },
                            placeholder = "Введите имя ссылки",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 5.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        SettingsInputLabel("Ссылка")
                        UIKit.Input(
                            value = link,
                            onValueChange = { link = it.take(150) },
                            placeholder = "https://example.com",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 5.dp)
                        )
                    }
                }
            }

            if (isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        UIKit.Button(
                            title = "Удалить",
                            onClick = {
                                linkId?.let { currentLinkId ->
                                    if (!processing) {
                                        processing = true
                                        scope.launch {
                                            val result = onDelete(currentLinkId)
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "Ссылка удалена", Toast.LENGTH_SHORT).show()
                                                onClose()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    result.message ?: "Не удалось удалить ссылку",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            processing = false
                                        }
                                    }
                                }
                            },
                            enabled = !processing,
                            loading = false,
                            variant = ElementButtonVariant.Soft,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        UIKit.Button(
                            title = "Изменить",
                            onClick = {
                                if (!processing && canSubmit) {
                                    processing = true
                                    scope.launch {
                                        val result = onSave(linkId, title, link)
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "Ссылка изменена", Toast.LENGTH_SHORT).show()
                                            onClose()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                result.message ?: "Не удалось изменить ссылку",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        processing = false
                                    }
                                }
                            },
                            enabled = !processing && canSubmit,
                            loading = processing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                UIKit.Button(
                    title = "Добавить",
                    onClick = {
                        if (!processing && canSubmit) {
                            processing = true
                            scope.launch {
                                val result = onSave(null, title, link)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Ссылка добавлена", Toast.LENGTH_SHORT).show()
                                    onClose()
                                } else {
                                    Toast.makeText(
                                        context,
                                        result.message ?: "Не удалось добавить ссылку",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                processing = false
                            }
                        }
                    },
                    enabled = !processing && canSubmit,
                    loading = processing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsChangeButtons(
    onUpload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonWidth: Dp = 112.dp,
    cornerRadius: Dp = 8.dp,
    buttonHeight: Dp = 30.dp,
    buttonSpacing: Dp = 1.dp,
    buttonTextFontSize: TextUnit = 13.sp,
    flatLeftEdge: Boolean = false
) {
    val isLightTheme = ElementUiPalette.Body.luminance() > 0.5f
    val uploadBg = if (isLightTheme) Color(0xFFFBFAFE) else ElementUiPalette.Block.copy(alpha = 0.96f)
    val deleteBg = if (isLightTheme) Color(0xFFDC727C) else Color(0xFF7E3338)
    val uploadText = if (isLightTheme) Color(0xFF45424E) else ElementUiPalette.TextPrimary
    val deleteText = Color.White
    val uploadBorder = ElementUiPalette.scaledBorder(0.95f)

    val leftCorner = if (flatLeftEdge) 0.dp else cornerRadius
    val contentStartPadding = if (flatLeftEdge) 42.dp else 10.dp
    val contentEndPadding = 12.dp
    val contentAlignment = if (flatLeftEdge) Alignment.CenterStart else Alignment.Center
    Column(
        modifier = modifier.clip(
            RoundedCornerShape(
                topStart = leftCorner,
                bottomStart = leftCorner,
                topEnd = cornerRadius,
                bottomEnd = cornerRadius
            )
        ),
        verticalArrangement = Arrangement.spacedBy(buttonSpacing)
    ) {
        Box(
            modifier = Modifier
                .width(buttonWidth)
                .height(buttonHeight)
                .clip(RoundedCornerShape(topStart = leftCorner, topEnd = cornerRadius))
                .background(uploadBg)
                .border(
                    width = 1.dp,
                    color = uploadBorder,
                    shape = RoundedCornerShape(topStart = leftCorner, topEnd = cornerRadius)
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onUpload() }
                .padding(start = contentStartPadding, end = contentEndPadding),
            contentAlignment = contentAlignment
        ) {
            Text(
                text = "Изменить",
                color = uploadText,
                fontSize = buttonTextFontSize,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .width(buttonWidth)
                .height(buttonHeight)
                .clip(RoundedCornerShape(bottomStart = leftCorner, bottomEnd = cornerRadius))
                .background(deleteBg)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDelete() }
                .padding(start = contentStartPadding, end = contentEndPadding),
            contentAlignment = contentAlignment
        ) {
            Text(
                text = "Удалить",
                color = deleteText,
                fontSize = buttonTextFontSize,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettingsChangeQuestion(
    visible: Boolean,
    loading: Boolean,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    if (!visible) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            UIKit.Button(
                title = "Применить",
                onClick = onApply,
                enabled = !loading,
                loading = loading,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            UIKit.Button(
                title = "Отменить",
                onClick = onCancel,
                enabled = !loading,
                variant = ElementButtonVariant.Soft,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SettingsInputLabel(text: String) {
    Text(
        text = text,
        color = ElementUiPalette.TextLite,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 11.dp, bottom = 0.dp)
    )
}

private val SettingsSectionTitleStartPadding = 10.dp

@Composable
private fun SettingsPartitionTitle(title: String) {
    Text(
        text = title,
        color = ElementUiPalette.TextLite,
        fontSize = SettingsModalSectionTitleTextSize,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = SettingsSectionTitleStartPadding, top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsModalAlertTitle(text: String) {
    Text(
        text = text,
        color = ElementUiPalette.TextPrimary,
        fontSize = SettingsModalPrimaryTextSize,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun SettingsModalAlertMessage(text: String) {
    Text(
        text = text,
        color = ElementUiPalette.TextSecondary,
        fontSize = SettingsModalSecondaryTextSize
    )
}

@Composable
private fun SettingsBuildFooter() {
    val isLightTheme = ElementUiPalette.Body.luminance() > 0.5f
    val signaturePulse = rememberInfiniteTransition(label = "settings-footer-signature")
    val signatureTone by signaturePulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "settings-footer-signature-tone"
    )
    val signatureAlpha by signaturePulse.animateFloat(
        initialValue = if (isLightTheme) 0.9f else 0.84f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "settings-footer-signature-alpha"
    )
    val signatureColor = lerp(
        start = if (isLightTheme) Color(0xFF696473) else Color(0xFF9B9BA3),
        stop = if (isLightTheme) ElementUiPalette.Accent.copy(alpha = 0.9f) else Color(0xFFF1F1F3),
        fraction = signatureTone
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "Element by moretti",
            color = signatureColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.graphicsLayer(alpha = signatureAlpha)
        )
        Text(
//            text = "Version ${BuildConfig.VERSION_NAME}",
            text = "Version 1.2 Music",
            color = if (isLightTheme) {
                ElementUiPalette.TextSecondary.copy(alpha = 0.82f)
            } else {
                ElementUiPalette.TextLite
            },
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SettingsMenuGroup(
    entries: List<SettingsMenuEntry>,
    onClick: (SettingsMenuEntry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.Block)
    ) {
        entries.forEachIndexed { index, entry ->
            SettingsMenuRow(
                entry = entry,
                onClick = { onClick(entry) }
            )
            if (index < entries.lastIndex) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ElementUiPalette.scaledBorder(0.65f))
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuRow(
    entry: SettingsMenuEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() }
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(entry.color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = entry.iconRes),
                contentDescription = entry.type,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }

        Text(
            text = entry.label,
            color = ElementUiPalette.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = ElementUiPalette.TextLite
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = ElementUiPalette.TextPrimary,
            fontSize = SettingsModalPrimaryTextSize,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsTransparencyRow(
    title: String,
    currentMode: TransparencyMode,
    onModeChanged: (TransparencyMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = ElementUiPalette.TextPrimary,
                fontSize = SettingsModalPrimaryTextSize,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = when (currentMode) {
                        TransparencyMode.ADAPTIVE -> "Адаптивная"
                        TransparencyMode.OPAQUE -> "Непрозрачные"
                        TransparencyMode.TRANSPARENT -> "Прозрачные"
                    },
                    color = ElementUiPalette.TextSecondary,
                    fontSize = SettingsModalPrimaryTextSize
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ElementUiPalette.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TransparencyMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                onModeChanged(mode)
                                expanded = false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { 
                                onModeChanged(mode)
                                expanded = false
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (mode) {
                                TransparencyMode.ADAPTIVE -> "Адаптивная (рекомендуется)"
                                TransparencyMode.OPAQUE -> "Непрозрачные элементы"
                                TransparencyMode.TRANSPARENT -> "Прозрачные элементы"
                            },
                            color = ElementUiPalette.TextPrimary,
                            fontSize = SettingsModalPrimaryTextSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPostsType(
    selected: PostsCategory,
    onSelect: (PostsCategory) -> Unit
) {
    val items = PostsCategory.entries
    UIKit.SegmentTabs(
        tabs = items.map { it.title },
        selectedIndex = items.indexOf(selected).coerceAtLeast(0),
        onSelect = { index -> onSelect(items[index]) }
    )
}

@Composable
private fun SettingsCover(
    asset: PostImageAsset?,
    preview: ImageBitmap?,
    refreshToken: Int,
    awaitRemoteRefresh: Boolean,
    isUploading: Boolean,
    homeGateway: HomeGateway,
    onRemoteBitmapReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap by rememberSettingsImageBitmap(
        asset = asset,
        homeGateway = homeGateway,
        refreshToken = refreshToken
    )
    val shownBitmap = preview ?: bitmap

    LaunchedEffect(awaitRemoteRefresh, bitmap) {
        if (awaitRemoteRefresh && bitmap != null) {
            onRemoteBitmapReady()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFC5C0D3),
                        Color(0xFF9E98AF)
                    )
                )
            )
    ) {
        if (shownBitmap != null) {
            Image(
                bitmap = shownBitmap,
                contentDescription = "cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                UIKit.Loader(size = 22)
            }
        }
    }
}

@Composable
private fun SettingsAvatar(
    name: String,
    asset: PostImageAsset?,
    preview: ImageBitmap?,
    refreshToken: Int,
    awaitRemoteRefresh: Boolean,
    isUploading: Boolean,
    homeGateway: HomeGateway,
    onRemoteBitmapReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bitmap by rememberSettingsImageBitmap(
        asset = asset,
        homeGateway = homeGateway,
        refreshToken = refreshToken
    )
    val shownBitmap = preview ?: bitmap

    LaunchedEffect(awaitRemoteRefresh, bitmap) {
        if (awaitRemoteRefresh && bitmap != null) {
            onRemoteBitmapReady()
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF7F6EB0), Color(0xFFA19DB1)))),
        contentAlignment = Alignment.Center
    ) {
        if (shownBitmap != null) {
            Image(
                bitmap = shownBitmap,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }

        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                UIKit.Loader(size = 18)
            }
        }
    }
}

@Composable
private fun SettingsSessionsModal(
    authGateway: AuthGateway,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var processing by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var currentSession by remember { mutableStateOf<AuthSession?>(null) }
    var otherSessions by remember { mutableStateOf<List<AuthSession>>(emptyList()) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var pendingTerminateAll by remember { mutableStateOf(false) }

    fun applySessions(current: AuthSession?, all: List<AuthSession>) {
        val safeCurrent = current ?: all.firstOrNull()
        currentSession = safeCurrent
        otherSessions = all
            .filter { safeCurrent == null || it.id != safeCurrent.id }
            .sortedBy { it.id }
    }

    fun loadSessions() {
        scope.launch {
            loading = true
            errorText = null
            val result = runCatching { authGateway.loadSessions() }.getOrNull()
            if (result?.isSuccess == true) {
                applySessions(result.currentSession, result.sessions)
                errorText = null
            } else {
                errorText = result?.message ?: "Не удалось загрузить сессии"
            }
            loading = false
        }
    }

    fun terminateSession(sessionId: String) {
        if (processing) return
        processing = true
        scope.launch {
            val result = runCatching { authGateway.deleteSession(sessionId) }.getOrNull()
            if (result?.isSuccess == true) {
                otherSessions = otherSessions.filterNot { it.id == sessionId }
                errorText = null
            } else {
                errorText = result?.message ?: "Не удалось завершить сессию"
            }
            processing = false
        }
    }

    fun sessionsDeletedText(count: Int): String {
        val mod10 = count % 10
        val mod100 = count % 100
        val suffix = when {
            mod10 == 1 && mod100 != 11 -> "сессия завершена"
            mod10 in 2..4 && mod100 !in 12..14 -> "сессии завершено"
            else -> "сессий завершено"
        }
        return "$count $suffix"
    }

    fun terminateAllSessions() {
        if (processing || otherSessions.isEmpty()) return
        processing = true
        scope.launch {
            val snapshot = otherSessions.toList()
            var deletedCount = 0
            snapshot.forEach { session ->
                val result = runCatching { authGateway.deleteSession(session.id) }.getOrNull()
                if (result?.isSuccess == true) {
                    deletedCount += 1
                }
            }
            otherSessions = emptyList()
            processing = false
            errorText = if (deletedCount > 0) {
                Toast.makeText(context, sessionsDeletedText(deletedCount), Toast.LENGTH_SHORT).show()
                null
            } else {
                "Не удалось завершить сессии"
            }
        }
    }

    LaunchedEffect(Unit) {
        loadSessions()
    }

    UIKit.RoutedModal(
        title = "Сессии",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 180.dp, height = 136.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SessionSvgImage(
                        svg = SettingsSessionsSvg.Top,
                        width = 156.dp,
                        height = 122.dp
                    )
                }

                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        UIKit.Loader(size = 26)
                    }
                } else {
                    currentSession?.let { current ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            SettingsSessionsSectionTitle("Текущая сессия")
                            SettingsSessionsGroup {
                                SettingsSessionRow(
                                    session = current,
                                    isCurrent = true,
                                    onTerminate = {}
                                )
                            }
                        }
                    }

                    if (currentSession == null && otherSessions.isEmpty()) {
                        UIKit.Block(
                            modifier = Modifier.fillMaxWidth(),
                            showShadow = false
                        ) {
                            Text(
                                text = "Активных сессий не найдено",
                                color = ElementUiPalette.TextLite,
                                fontSize = SettingsModalSecondaryTextSize
                            )
                        }
                    }

                    if (otherSessions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            SettingsSessionsSectionTitle("Все сессии")
                            SettingsSessionsGroup {
                                otherSessions.forEachIndexed { index, session ->
                                    SettingsSessionRow(
                                        session = session,
                                        isCurrent = false,
                                        onTerminate = { pendingDeleteId = session.id }
                                    )
                                    if (index < otherSessions.lastIndex) {
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(ElementUiPalette.scaledBorder(0.6f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!loading && otherSessions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFF5D5D))
                        .clickable(enabled = !processing) { pendingTerminateAll = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Завершить все сессии",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!errorText.isNullOrBlank()) {
                UIKit.Block(
                    modifier = Modifier.fillMaxWidth(),
                    showShadow = false
                ) {
                    Text(
                        text = errorText.orEmpty(),
                        color = ElementUiPalette.Error,
                        fontSize = SettingsModalSecondaryTextSize
                    )
                }
            }
        }
    }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { SettingsModalAlertTitle("Вы уверенны?") },
            text = { SettingsModalAlertMessage("Вы уверенны что хотите завершить сессию?") },
            properties = DialogProperties(decorFitsSystemWindows = false),
            confirmButton = {
                TextButton(
                    onClick = {
                        val sessionId = pendingDeleteId ?: return@TextButton
                        pendingDeleteId = null
                        terminateSession(sessionId)
                    }
                ) { Text("Завершить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Отмена") }
            }
        )
    }

    if (pendingTerminateAll) {
        AlertDialog(
            onDismissRequest = { pendingTerminateAll = false },
            title = { SettingsModalAlertTitle("Вы уверенны?") },
            text = { SettingsModalAlertMessage("Вы уверены что хотите завершить все сессии?") },
            properties = DialogProperties(decorFitsSystemWindows = false),
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingTerminateAll = false
                        terminateAllSessions()
                    }
                ) { Text("Завершить все") }
            },
            dismissButton = {
                TextButton(onClick = { pendingTerminateAll = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun SettingsSessionsSectionTitle(title: String) {
    Text(
        text = title,
        color = ElementUiPalette.TextLite,
        fontSize = SettingsModalSectionTitleTextSize,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = SettingsSectionTitleStartPadding)
    )
}

@Composable
private fun SettingsSessionsGroup(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElementUiPalette.Block)
    ) {
        content()
    }
}

@Composable
private fun SettingsSessionRow(
    session: AuthSession,
    isCurrent: Boolean,
    onTerminate: () -> Unit
) {
    val (iconSvg, name) = remember(session.deviceType) {
        when (session.deviceType) {
            1 -> SettingsSessionsSvg.Safari to "Браузер"
            2 -> SettingsSessionsSvg.Android to "Android"
            3 -> SettingsSessionsSvg.Apple to "iOS"
            4 -> SettingsSessionsSvg.Windows to "Windows"
            else -> SettingsSessionsSvg.Anonymous to "Аноним"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(35.dp)
                .clip(CircleShape)
                .background(ElementUiPalette.BlockSoft),
            contentAlignment = Alignment.Center
        ) {
            SessionSvgImage(
                svg = iconSvg,
                width = 22.dp,
                height = 22.dp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = ElementUiPalette.TextPrimary,
                fontSize = SettingsModalPrimaryTextSize,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = session.device?.ifBlank { "Element device" } ?: "Element device",
                color = ElementUiPalette.TextSecondary,
                fontSize = SettingsModalSecondaryTextSize
            )
        }

        if (!isCurrent) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTerminate),
                contentAlignment = Alignment.Center
            ) {
                SessionSvgImage(
                    svg = SettingsSessionsSvg.Close,
                    width = 22.dp,
                    height = 22.dp
                )
            }
        }
    }
}

@Composable
private fun SessionSvgImage(
    svg: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val widthPx = remember(width, density) { with(density) { width.roundToPx().coerceAtLeast(1) } }
    val heightPx = remember(height, density) { with(density) { height.roundToPx().coerceAtLeast(1) } }
    val bitmap by rememberSessionSvgBitmap(svg = svg, widthPx = widthPx, heightPx = heightPx)

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier.size(width = width, height = height),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier
                .size(width = width, height = height)
                .clip(RoundedCornerShape(6.dp))
                .background(ElementUiPalette.BlockSoft)
        )
    }
}

@Composable
private fun rememberSessionSvgBitmap(
    svg: String,
    widthPx: Int,
    heightPx: Int
) = produceState<ImageBitmap?>(initialValue = null, key1 = svg, key2 = widthPx, key3 = heightPx) {
    value = withContext(Dispatchers.Default) {
        runCatching {
            val svgDoc = SVG.getFromString(svg)
            val viewBox = svgDoc.documentViewBox
            if (viewBox == null) {
                val docWidth = svgDoc.documentWidth
                val docHeight = svgDoc.documentHeight
                if (docWidth > 0f && docHeight > 0f) {
                    svgDoc.setDocumentViewBox(0f, 0f, docWidth, docHeight)
                }
            }
            svgDoc.setDocumentWidth("100%")
            svgDoc.setDocumentHeight("100%")
            val picture = svgDoc.renderToPicture(widthPx, heightPx)
            val bitmap = android.graphics.Bitmap.createBitmap(
                widthPx,
                heightPx,
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = AndroidCanvas(bitmap)
            canvas.drawPicture(
                picture,
                android.graphics.RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
            )
            bitmap.asImageBitmap()
        }.getOrNull()
    }
}

@Composable
private fun rememberSettingsImageBitmap(
    asset: PostImageAsset?,
    homeGateway: HomeGateway,
    refreshToken: Int
) = produceState<ImageBitmap?>(initialValue = null, key1 = asset?.cacheKey, key2 = refreshToken) {
    val target = asset ?: return@produceState
    val bytes = runCatching { homeGateway.loadImageBytes(target) }.getOrNull() ?: return@produceState
    if (bytes.isEmpty()) return@produceState

    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    value = bitmap?.asImageBitmap()
}
