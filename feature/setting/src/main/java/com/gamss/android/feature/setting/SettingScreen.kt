package com.gamss.android.feature.setting

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.domain.user.NicknamePolicy
import com.gamss.android.domain.user.UserProfile
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun SettingScreen(
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        Toast.makeText(context, sideEffect.toMessage(), Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "Setting 기능 테스트",
            style = MaterialTheme.typography.headlineSmall,
        )

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }

        GetUserInfoSection(
            userProfile = state.userProfile,
            onLoadUserInfo = viewModel::loadUserInfo,
            enabled = !state.isLoading,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        UpdateNicknameSection(
            nicknameInput = state.nicknameInput,
            onNicknameInputChange = viewModel::onNicknameInputChange,
            onUpdateNickname = viewModel::updateNickname,
            enabled = !state.isLoading,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        deleteAccountSection(
            onDeleteAccount = viewModel::deleteUserAccount,
            enabled = !state.isLoading,
        )
    }
}

@Composable
private fun GetUserInfoSection(
    userProfile: UserProfile?,
    onLoadUserInfo: () -> Unit,
    enabled: Boolean,
) {
    Text(
        modifier = Modifier.padding(top = 16.dp),
        text = "1. 사용자 정보 조회",
        style = MaterialTheme.typography.titleMedium,
    )
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        onClick = onLoadUserInfo,
        enabled = enabled,
    ) {
        Text("내 정보 조회")
    }
    Text(
        modifier = Modifier.padding(top = 8.dp),
        text = if (userProfile != null) {
            "id: ${userProfile.id}\n" +
                "email: ${userProfile.email}\n" +
                "nickname: ${userProfile.nickname}\n" +
                "status: ${userProfile.status}\n" +
                "createdAt: ${userProfile.createdAt}"
        } else {
            "조회된 정보가 없어요"
        },
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun UpdateNicknameSection(
    nicknameInput: String,
    onNicknameInputChange: (String) -> Unit,
    onUpdateNickname: () -> Unit,
    enabled: Boolean,
) {
    Text(
        text = "2. 닉네임 변경",
        style = MaterialTheme.typography.titleMedium,
    )
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        value = nicknameInput,
        onValueChange = { nickname ->
            if (nickname.length <= NicknamePolicy.MAX_LENGTH) {
                onNicknameInputChange(nickname)
            }
        },
        label = { Text("닉네임") },
    )
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        onClick = onUpdateNickname,
        enabled = enabled,
    ) {
        Text("닉네임 변경")
    }
}

@Composable
private fun deleteAccountSection(
    onDeleteAccount: () -> Unit,
    enabled: Boolean,
) {
    Text(
        text = "3. 탈퇴",
        style = MaterialTheme.typography.titleMedium,
    )
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        onClick = onDeleteAccount,
        enabled = enabled,
    ) {
        Text("회원 탈퇴")
    }
}

private fun SettingSideEffect.toMessage(): String =
    when (this) {
        SettingSideEffect.LoadUserInfoFailure -> "사용자 정보를 불러오지 못했어요"
        SettingSideEffect.UpdateNicknameSuccess -> "닉네임이 변경되었어요"
        is SettingSideEffect.UpdateNicknameFailure -> reason.toMessage()
        SettingSideEffect.DeleteAccountFailure -> "회원 탈퇴에 실패했어요"
    }

private fun NicknameFailureReason.toMessage(): String =
    when (this) {
        NicknameFailureReason.MISSING -> "닉네임을 입력해주세요"
        NicknameFailureReason.INVALID_LENGTH -> "닉네임은 2~20자로 입력해주세요"
        NicknameFailureReason.INVALID_NICKNAME -> "사용할 수 없는 닉네임이에요"
        NicknameFailureReason.NETWORK -> "네트워크 연결을 확인해주세요"
        NicknameFailureReason.UNKNOWN -> "닉네임 변경에 실패했어요"
    }
