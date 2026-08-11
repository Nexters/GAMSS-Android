package com.gamss.android.feature.login

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.feature.login.auth.GoogleCredentialResult
import com.gamss.android.feature.login.auth.rememberGoogleCredentialLauncher
import com.gamss.android.feature.login.component.GoogleSignInButton
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

private const val CHARACTERS_ASPECT_RATIO = 340f / 260f

// Figma 로그인 프레임 폭. 이보다 넓은 화면에서는 내용을 가운데 정렬한다.
private val DesignWidth = 402.dp

@Composable
fun LoginScreen(
    googleWebClientId: String,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val googleCredentialLauncher = rememberGoogleCredentialLauncher(googleWebClientId)

    LaunchedEffect(state.googleSignInRequestId) {
        if (state.googleSignInRequestId == null) return@LaunchedEffect

        when (val result = googleCredentialLauncher.launch()) {
            is GoogleCredentialResult.Success ->
                viewModel.onGoogleCredentialResolved(result.idToken)

            GoogleCredentialResult.Cancelled ->
                viewModel.onGoogleSignInCancelled()

            GoogleCredentialResult.Failure ->
                viewModel.onGoogleSignInFailed()
        }
    }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is LoginSideEffect.ShowToast ->
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
        }
    }

    LoginContent(
        isLoading = state.isLoading,
        onGoogleSignInClick = viewModel::requestGoogleSignIn,
    )
}

@Composable
private fun LoginContent(
    isLoading: Boolean,
    onGoogleSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 세로 간격이 모두 고정값이라 가로 모드나 작은 화면에서는 높이가 부족하다.
    // 스크롤이 없으면 Column 이 자식을 순서대로 측정하면서 버튼 높이를 0 까지 깎는다.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GamssTheme.colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 가로 모드나 태블릿에서 이미지와 버튼이 화면 폭만큼 늘어나지 않도록 디자인 기준 폭으로 제한한다.
        Column(
            modifier = Modifier.widthIn(max = DesignWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(130.dp))
            Image(
                painter = painterResource(R.drawable.img_login_logo),
                contentDescription = stringResource(R.string.login_logo_description),
                modifier = Modifier.size(width = 120.dp, height = 40.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.login_subtitle),
                style = GamssTheme.typography.subtitle2,
                color = GamssTheme.colors.gray950,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(R.drawable.img_login_characters),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 31.dp)
                    .fillMaxWidth()
                    .aspectRatio(CHARACTERS_ASPECT_RATIO),
            )
            Spacer(modifier = Modifier.height(54.dp))
            GoogleSignInButton(
                isLoading = isLoading,
                onClick = onGoogleSignInClick,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
            Spacer(modifier = Modifier.height(54.dp))
        }
    }
}

// 다크 프리뷰는 darkTheme 플래그가 아니라 uiMode 로 지정한다.
// -night 리소스 해석이 시스템 설정을 따르므로, 플래그만 바꾸면 색과 에셋이 어긋난다.
@Preview(name = "Login - Light", showBackground = true)
@Preview(name = "Login - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
@Suppress("UnusedPrivateMember")
private fun LoginContentPreview() {
    GamssTheme {
        LoginContent(isLoading = false, onGoogleSignInClick = {})
    }
}

@Preview(name = "Login - Loading", showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun LoginContentLoadingPreview() {
    GamssTheme {
        LoginContent(isLoading = true, onGoogleSignInClick = {})
    }
}
