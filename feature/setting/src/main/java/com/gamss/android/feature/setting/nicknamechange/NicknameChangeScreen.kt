package com.gamss.android.feature.setting.nicknamechange

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.button.GamssButton
import com.gamss.android.core.designsystem.button.GamssButtonVariant
import com.gamss.android.core.designsystem.textfield.GamssTextField
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.topnavigation.GamssTopNavigation
import com.gamss.android.domain.user.NicknamePolicy
import com.gamss.android.feature.setting.R
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun NicknameChangeScreen(
    currentNickname: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NicknameChangeViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(currentNickname) { viewModel.start(currentNickname) }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            NicknameChangeSideEffect.UpdateSuccess -> {
                Toast.makeText(context, "닉네임이 변경되었어요", Toast.LENGTH_SHORT).show()
                onBackClick()
            }

            is NicknameChangeSideEffect.UpdateFailure -> {
                Toast.makeText(context, sideEffect.reason.toMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val trimmedInput = state.nicknameInput.trim()
    val canSave = trimmedInput.isNotEmpty() &&
        trimmedInput != state.originalNickname &&
        trimmedInput.length in NicknamePolicy.MIN_LENGTH..NicknamePolicy.MAX_LENGTH
    // 길이를 벗어난 입력도 막지 않고 그대로 받아 안내 문구로 알린다. 저장은 canSave 가 막는다.
    // 판정 기준은 canSave 와 같은 공백 제외 길이다.
    val lengthErrorMessage = when {
        state.nicknameInput.isEmpty() -> null

        trimmedInput.length < NicknamePolicy.MIN_LENGTH ->
            stringResource(R.string.nickname_change_input_error_min_length)

        trimmedInput.length > NicknamePolicy.MAX_LENGTH ->
            stringResource(R.string.nickname_change_input_error_max_length)

        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GamssTheme.colors.background)
            .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars)),
    ) {
        GamssTopNavigation(
            title = stringResource(R.string.account_info_nickname_change),
            showLeftIcon = true,
            onLeftIconClick = onBackClick,
        )

        GamssTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(top = GamssTheme.spacing.spacing300),
            label = stringResource(R.string.nickname_change_input_label),
            placeholder = stringResource(R.string.nickname_change_input_placeholder),
            errorMessage = lengthErrorMessage,
            value = state.nicknameInput,
            onValueChange = viewModel::onNicknameInputChange,
        )

        Spacer(modifier = Modifier.weight(1f))

        GamssButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenHorizontalPadding)
                .padding(bottom = GamssTheme.spacing.spacing200),
            label = stringResource(R.string.nickname_change_save),
            onClick = viewModel::saveNickname,
            variant = GamssButtonVariant.Primary,
            enabled = canSave,
        )
    }
}

private val ScreenHorizontalPadding = 18.dp

private fun NicknameFailureReason.toMessage(): String =
    when (this) {
        NicknameFailureReason.MISSING -> "닉네임을 입력해주세요"
        NicknameFailureReason.INVALID_LENGTH -> "닉네임은 2~10자로 입력해주세요"
        NicknameFailureReason.INVALID_NICKNAME -> "사용할 수 없는 닉네임이에요"
        NicknameFailureReason.NETWORK -> "네트워크 연결을 확인해주세요"
        NicknameFailureReason.UNKNOWN -> "닉네임 변경에 실패했어요"
    }
