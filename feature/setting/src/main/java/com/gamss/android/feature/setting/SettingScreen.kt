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
        when (sideEffect) {
            is SettingSideEffect.ShowToast ->
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
        }
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
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        UpdateNicknameSection(
            nicknameInput = state.nicknameInput,
            onNicknameInputChange = viewModel::onNicknameInputChange,
            onUpdateNickname = viewModel::updateNickname,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

        SecessionSection(onSecession = viewModel::secession)
    }
}

@Composable
private fun GetUserInfoSection(
    userProfile: UserProfile?,
    onLoadUserInfo: () -> Unit,
) {
    Text(
        modifier = Modifier.padding(top = 16.dp),
        text = "1. GetUserInfoUseCase",
        style = MaterialTheme.typography.titleMedium,
    )
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        onClick = onLoadUserInfo,
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
) {
    Text(
        text = "2. UpdateNicknameUseCase",
        style = MaterialTheme.typography.titleMedium,
    )
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        value = nicknameInput,
        onValueChange = onNicknameInputChange,
        label = { Text("닉네임") },
    )
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        onClick = onUpdateNickname,
    ) {
        Text("닉네임 변경")
    }
}

@Composable
private fun SecessionSection(
    onSecession: () -> Unit,
) {
    Text(
        text = "3. SecessionUseCase",
        style = MaterialTheme.typography.titleMedium,
    )
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        onClick = onSecession,
    ) {
        Text("회원 탈퇴")
    }
}
