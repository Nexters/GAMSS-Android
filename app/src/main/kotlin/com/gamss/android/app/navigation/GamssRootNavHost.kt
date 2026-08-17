package com.gamss.android.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.gamss.android.app.R
import com.gamss.android.app.main.MainScreen
import com.gamss.android.app.main.MainViewModel
import com.gamss.android.domain.auth.SessionState
import com.gamss.android.feature.login.LoginScreen
import com.gamss.android.feature.login.navigation.LoginKey
import org.orbitmvi.orbit.compose.collectAsState

@Composable
fun GamssRootNavHost(
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val state by mainViewModel.collectAsState()

    when (state.sessionState) {
        SessionState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        else -> RootNavDisplay(
            sessionState = state.sessionState,
            useCardFeature = state.useCardFeature,
        )
    }
}

@Composable
private fun RootNavDisplay(
    sessionState: SessionState,
    useCardFeature: Boolean,
) {
    val initialKey = if (sessionState == SessionState.Authenticated) MainKey else LoginKey
    val backStack = rememberNavBackStack(initialKey)

    LaunchedEffect(sessionState) {
        val destination = when (sessionState) {
            SessionState.Authenticated -> MainKey
            SessionState.Unauthenticated -> LoginKey
            SessionState.Loading -> return@LaunchedEffect
        }
        if (backStack.lastOrNull() != destination) {
            backStack.clear()
            backStack.add(destination)
        }
    }

    NavDisplay(
        entries = backStack.map(
            entryProvider {
                entry<LoginKey> {
                    LoginScreen(
                        googleWebClientId = stringResource(R.string.default_web_client_id),
                    )
                }
                entry<MainKey> { MainScreen(useCardFeature = useCardFeature) }
            },
        ),

        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        popTransitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
    )
}
