package com.gamss.android.feature.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
            contentAlignment = Alignment.Center,
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    Button(
                        onClick = viewModel::logout,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GamssTheme.colors.green,
                            contentColor = GamssTheme.colors.gray025,
                        ),
                    ) {
                        Text(
                            text = "로그아웃",
                            style = GamssTheme.typography.subtitle4,
                        )
                    }
                }
            }
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
