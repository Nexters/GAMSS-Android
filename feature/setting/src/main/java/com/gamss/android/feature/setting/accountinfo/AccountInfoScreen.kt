package com.gamss.android.feature.setting.accountinfo

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.dialog.GamssDialog
import com.gamss.android.core.designsystem.dialog.GamssDialogAction
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.feature.setting.R
import com.gamss.android.feature.setting.component.SettingListItem
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun AccountInfoScreen(
    onBackClick: () -> Unit,
    onNicknameChangeClick: (currentNickname: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AccountInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    // 닉네임 변경 화면에서 저장하고 돌아왔을 때 최신 정보를 다시 불러오기 위해
    // ViewModel init이 아니라 화면이 보일 때마다 실행되는 LaunchedEffect로 조회한다.
    LaunchedEffect(Unit) { viewModel.loadUserInfo() }

    viewModel.collectSideEffect { sideEffect ->
        Toast.makeText(context, sideEffect.toMessage(), Toast.LENGTH_SHORT).show()
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteUserInfoDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        LogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onLogout = {
                showLogoutDialog = false
                viewModel.logout()
            },
        )
    }

    if (showDeleteUserInfoDialog) {
        DeleteUserInfoDialog(
            onDismiss = { showDeleteUserInfoDialog = false },
            onDeleteUserInfo = {
                showDeleteUserInfoDialog = false
                viewModel.deleteUserAccount()
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GamssTheme.colors.gray025),
    ) {
        GamssTopNavigation(
            modifier = Modifier.padding(bottom = GamssTheme.spacing.spacing150),
            title = stringResource(R.string.setting_list_user_account),
            showLeftIcon = true,
            onLeftIconClick = onBackClick,
        )

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(GamssTheme.spacing.spacing300)) {
                SettingListItem(
                    title = stringResource(R.string.account_info_nickname_change),
                    onClick = { onNicknameChangeClick(state.userProfile?.nickname.orEmpty()) },
                    additionalInfo = state.userProfile?.nickname,
                )
                SettingListItem(
                    title = stringResource(R.string.account_info_email),
                    onClick = {},
                    additionalInfo = state.userProfile?.email,
                )
                SettingListItem(
                    title = stringResource(R.string.account_info_logout),
                    onClick = { showLogoutDialog = true },
                )
                SettingListItem(
                    title = stringResource(R.string.account_info_delete_user_info),
                    onClick = { showDeleteUserInfoDialog = true },
                    contentColor = GamssTheme.colors.red
                )
            }
        }
    }
}

@Composable
private fun LogoutDialog(
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
) {
    GamssDialog(
        title = stringResource(R.string.account_info_logout_dialog_title),
        primaryAction = GamssDialogAction(
            label = stringResource(R.string.account_info_logout_dialog_dismiss_button_label),
            onClick = onDismiss,
        ),
        secondaryAction = GamssDialogAction(
            label = stringResource(R.string.account_info_logout_dialog_action_button_label),
            onClick = onLogout,
            variant = GamssButtonVariant.Secondary,
        ),
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun DeleteUserInfoDialog(
    onDismiss: () -> Unit,
    onDeleteUserInfo: () -> Unit,
) {
    GamssDialog(
        title = stringResource(R.string.account_info_delete_user_info_dialog_title),
        subtitle = stringResource(R.string.account_info_delete_user_info_dialog_subtitle),
        primaryAction = GamssDialogAction(
            label = stringResource(R.string.account_info_delete_user_info_dialog_action_button_label),
            onClick = onDeleteUserInfo,
            variant = GamssButtonVariant.Destructive,
        ),
        secondaryAction = GamssDialogAction(
            label = stringResource(R.string.account_info_delete_user_info_dialog_dismiss_button_label),
            onClick = onDismiss,
            variant = GamssButtonVariant.Secondary,
        ),
        onDismissRequest = onDismiss,
    )
}

private fun AccountInfoSideEffect.toMessage(): String =
    when (this) {
        AccountInfoSideEffect.LoadUserInfoFailure -> "사용자 정보를 불러오지 못했어요"
        AccountInfoSideEffect.DeleteAccountFailure -> "회원 탈퇴에 실패했어요"
        AccountInfoSideEffect.LogoutFailure -> "로그아웃에 실패했어요"
    }
