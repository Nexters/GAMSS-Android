package com.gamss.android.feature.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.theme.GamssTheme
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun HomeScreen(
    onNavigateToSetting: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is HomeSideEffect.ShowToast ->
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
            is HomeSideEffect.NavigateToSetting -> onNavigateToSetting()
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
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
                CircularProgressIndicator()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.greeting.ifEmpty { "GAMSS" },
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = viewModel::navigateToSetting) {
                        Text("설정으로 이동")
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

@Composable
private fun ThemeVerificationButton(
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = {},
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = GamssTheme.colors.green,
            contentColor = GamssTheme.colors.gray025,
        ),
    ) {
        Text(
            text = "GAMSS 버튼",
            style = GamssTheme.typography.subtitle4,
        )
    }
}

@Preview(name = "Button - Light", showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun LightThemeVerificationButtonPreview() {
    GamssTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .background(GamssTheme.colors.gray025)
                .padding(24.dp),
        ) {
            ThemeVerificationButton()
        }
    }
}

@Preview(name = "Button - Dark", showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun DarkThemeVerificationButtonPreview() {
    GamssTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(GamssTheme.colors.gray025)
                .padding(24.dp),
        ) {
            ThemeVerificationButton()
        }
    }
}
