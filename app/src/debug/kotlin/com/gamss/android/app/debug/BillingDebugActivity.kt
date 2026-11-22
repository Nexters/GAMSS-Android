package com.gamss.android.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gamss.android.app.R
import com.gamss.android.app.payment.PendingPurchaseVerifier
import com.gamss.android.app.payment.PurchaseScreen
import com.gamss.android.app.payment.observePaymentRecovery
import com.gamss.android.core.designsystem.component.GamssPaperBackground
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.domain.auth.AuthRepository
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.feature.login.LoginScreen
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** debug APK에만 포함되는 결제 테스트 진입점. 로그인 후 서버 검증까지 실제 흐름을 사용한다. */
@AndroidEntryPoint
class BillingDebugActivity : ComponentActivity() {
    @Inject
    lateinit var verifier: Lazy<PendingPurchaseVerifier>

    @Inject
    lateinit var auth: Lazy<AuthRepository>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observePaymentRecovery(verifier, auth)
        lifecycleScope.launch {
            if (auth.get().sessionState.value == SessionState.Loading) auth.get().restoreSession()
        }
        setContent {
            val session by auth.get().sessionState.collectAsStateWithLifecycle()
            GamssTheme {
                GamssPaperBackground {
                    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                        when (session) {
                            SessionState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                            SessionState.Unauthenticated -> LoginScreen(
                                googleWebClientId = stringResource(R.string.default_web_client_id),
                                onLoginSuccess = { _ -> },
                            )
                            SessionState.Authenticated -> PurchaseScreen(onBackClick = ::finish)
                        }
                    }
                }
            }
        }
    }
}
