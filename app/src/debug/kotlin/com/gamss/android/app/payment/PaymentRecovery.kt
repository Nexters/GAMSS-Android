package com.gamss.android.app.payment

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gamss.android.domain.auth.AuthRepository
import com.gamss.android.domain.auth.SessionState
import dagger.Lazy
import kotlinx.coroutines.launch

/** 로그인 완료와 매 onResume 모두 복구 조회를 일으킨다. 테스트 빌드에서만 활성화한다. */
internal fun ComponentActivity.observePaymentRecovery(
    verifier: Lazy<PendingPurchaseVerifier>,
    auth: Lazy<AuthRepository>,
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.RESUMED) {
            auth.get().sessionState.collect { session ->
                if (session == SessionState.Authenticated) verifier.get().onResume()
            }
        }
    }
}
