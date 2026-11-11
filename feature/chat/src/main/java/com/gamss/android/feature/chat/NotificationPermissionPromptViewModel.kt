package com.gamss.android.feature.chat

import androidx.lifecycle.ViewModel
import com.gamss.android.domain.push.MarkNotificationPermissionPromptedUseCase
import com.gamss.android.domain.push.ShouldPromptNotificationPermissionUseCase
import com.gamss.android.domain.push.SyncDeviceTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

data object RequestNotificationPermission

/** 대화를 두고 나가도 서버가 카드를 만들어 알리므로 새 대화를 열 때 알림 권한을 묻는다. */
@HiltViewModel
class NotificationPermissionPromptViewModel @Inject constructor(
    private val shouldPromptNotificationPermission: ShouldPromptNotificationPermissionUseCase,
    private val markNotificationPermissionPrompted: MarkNotificationPermissionPromptedUseCase,
    private val syncDeviceToken: SyncDeviceTokenUseCase,
) : ViewModel(),
    ContainerHost<Boolean, RequestNotificationPermission> {

    override val container = container<Boolean, RequestNotificationPermission>(false)

    fun onNewConversationOpened(shouldShowRationale: Boolean) = intent {
        if (shouldPromptNotificationPermission(shouldShowRationale)) reduce { true }
    }

    fun onDismiss() = intent {
        reduce { false }
    }

    fun onConfirm() = intent {
        if (close()) postSideEffect(RequestNotificationPermission)
    }

    /** 첫 거절만 rationale 이 true 가 된다. 선택 없이 닫으면 OS 도 거절로 세지 않으므로 기록하지 않는다. */
    fun onResult(granted: Boolean, shouldShowRationale: Boolean) = intent {
        if (!granted && shouldShowRationale) markNotificationPermissionPrompted()
        syncDeviceToken()
    }

    /** 검사와 닫기를 한 reduce 에서 해야 연타로 두 번 처리되지 않는다. */
    private suspend fun Syntax<Boolean, RequestNotificationPermission>.close(): Boolean {
        var closed = false
        reduce {
            closed = state
            false
        }
        return closed
    }
}
