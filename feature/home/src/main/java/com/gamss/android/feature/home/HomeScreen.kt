package com.gamss.android.feature.home

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is HomeSideEffect.ShowToast ->
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TokenUsageIndicator(
                tokenUsage = state.tokenUsage,
                isLoading = state.isTokenUsageLoading,
                // innerPadding 에 이미 상태바 inset 이 들어 있어 따로 더하지 않는다.
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
            )

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.greeting.ifEmpty { "GAMSS" },
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = viewModel::loadGreeting) {
                        Text("새로고침")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // 정식 설정 화면이 추가되기 전까지 사용하는 임시 로그아웃 버튼
                    Button(onClick = viewModel::logout) {
                        Text("로그아웃")
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenUsageIndicator(
    tokenUsage: TokenUsageUiModel?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    if (tokenUsage == null && !isLoading) return

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
    ) {
        // 이미 아는 사용량이 있으면 갱신 중에도 지우지 않고 그대로 보여준다.
        if (tokenUsage != null) {
            TokenUsageContent(tokenUsage)
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .size(18.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun TokenUsageContent(tokenUsage: TokenUsageUiModel) {
    val displayText = tokenUsage.toDisplayText()

    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = "오늘 토큰",
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = displayText.headline,
            style = MaterialTheme.typography.titleMedium,
        )
        displayText.supportingText?.let { supportingText ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        tokenUsage.usageRatio?.let { usageRatio ->
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { usageRatio.coerceIn(0f, 1f) },
                // fillMaxWidth 로 두면 배지 전체가 화면 폭까지 늘어난다.
                modifier = Modifier.width(96.dp),
                color = if (tokenUsage.exceeded) {
                    MaterialTheme.colorScheme.error
                } else {
                    ProgressIndicatorDefaults.linearColor
                },
            )
        }
    }
}
