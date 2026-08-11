package com.gamss.android.feature.setting.accountinfo

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.gamss.android.core.designsystem.dialog.GamssConfirmDialog
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
        GamssConfirmDialog(
            title = stringResource(R.string.account_info_logout_dialog_title),
            actionButtonLabel = stringResource(R.string.account_info_logout_dialog_action_button_label),
            dismissButtonLabel = stringResource(R.string.account_info_logout_dialog_dismiss_button_label),
            onActionClick = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismissClick = { showLogoutDialog = false },
            actionButtonVariant = GamssButtonVariant.Neutral,
        )
    }

    if (showDeleteUserInfoDialog) {
        GamssConfirmDialog(
            title = stringResource(R.string.account_info_delete_user_info_dialog_title),
            actionButtonLabel = stringResource(R.string.account_info_delete_user_info_dialog_action_button_label),
            dismissButtonLabel = stringResource(R.string.account_info_delete_user_info_dialog_dismiss_button_label),
            onActionClick = {
                showDeleteUserInfoDialog = false
                viewModel.deleteUserAccount()
            },
            onDismissClick = { showDeleteUserInfoDialog = false },
            actionButtonVariant = GamssButtonVariant.Destructive,
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        GamssTopNavigation(
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
            )
        }
    }
}

private fun AccountInfoSideEffect.toMessage(): String =
    when (this) {
        AccountInfoSideEffect.LoadUserInfoFailure -> "사용자 정보를 불러오지 못했어요"
        AccountInfoSideEffect.DeleteAccountFailure -> "회원 탈퇴에 실패했어요"
        AccountInfoSideEffect.LogoutFailure -> "로그아웃에 실패했어요"
    }
