package com.gamss.android.feature.setting.nicknamechange

import androidx.lifecycle.ViewModel
import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.domain.user.NicknameUpdateException
import com.gamss.android.domain.user.UpdateNicknameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class NicknameChangeViewModel @Inject constructor(
    private val updateNicknameUseCase: UpdateNicknameUseCase,
) : ViewModel(), ContainerHost<NicknameChangeState, NicknameChangeSideEffect> {

    override val container = container<NicknameChangeState, NicknameChangeSideEffect>(NicknameChangeState())

    /** NicknameChangeScreen의 LaunchedEffect(currentNickname)에서 최초 진입 시 호출한다. */
    fun start(currentNickname: String) = intent {
        if (state.originalNickname == currentNickname) return@intent
        reduce {
            state.copy(
                originalNickname = currentNickname,
                nicknameInput = currentNickname,
            )
        }
    }

    fun onNicknameInputChange(nickname: String) = intent {
        reduce { state.copy(nicknameInput = nickname) }
    }

    fun saveNickname() = intent {
        if (state.isSaving) return@intent

        reduce { state.copy(isSaving = true) }
        when (val result = updateNicknameUseCase(state.nicknameInput)) {
            is AppResult.Success -> {
                reduce { state.copy(isSaving = false) }
                postSideEffect(NicknameChangeSideEffect.UpdateSuccess)
            }

            is AppResult.Failure -> {
                reduce { state.copy(isSaving = false) }
                postSideEffect(NicknameChangeSideEffect.UpdateFailure(result.throwable.toNicknameFailureReason()))
            }
        }
    }
}

private fun Throwable.toNicknameFailureReason(): NicknameFailureReason =
    when (this) {
        is NicknameUpdateException.MissingNickname -> NicknameFailureReason.MISSING
        is NicknameUpdateException.InvalidLength -> NicknameFailureReason.INVALID_LENGTH
        is NicknameUpdateException.InvalidNickname -> NicknameFailureReason.INVALID_NICKNAME
        is ApiException.Network -> NicknameFailureReason.NETWORK
        else -> NicknameFailureReason.UNKNOWN
    }
