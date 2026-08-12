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
import androidx.compose.ui.graphics.ColorFilter
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

// 아래 값들은 Figma 로그인 프레임의 고유 치수라 GamssSpacing 스케일에 없다.
// 간격 성격의 값 중 스케일에 있는 것은 모두 GamssTheme.spacing 에서 꺼내 쓴다.
private val LogoWidth = 120.dp
private val LogoHeight = 40.dp
private val LogoTopSpacing = 130.dp

// 일러스트 에셋의 기준 크기. 최대 폭과 종횡비가 여기서 함께 파생된다.
private val CharactersWidth = 340.dp
private val CharactersHeight = 260.dp
private val CharactersAspectRatio = CharactersWidth / CharactersHeight

private val CharactersToButtonSpacing = 54.dp
private val ButtonBottomSpacing = 54.dp

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
    // 세로 간격이 모두 고정값이라 짧은 화면에서는 높이가 부족하다.
    // 스크롤이 없으면 Column 이 자식을 순서대로 측정하면서 버튼 높이를 0 까지 깎는다.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GamssTheme.colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(LogoTopSpacing))
        // 로고 벡터는 라이트 색으로 그려져 있고 테마 전환은 틴트로 처리한다.
        // 색만 다른 drawable-night 사본을 두면 패스 데이터가 통째로 중복된다.
        Image(
            painter = painterResource(R.drawable.img_login_logo),
            contentDescription = stringResource(R.string.login_logo_description),
            colorFilter = ColorFilter.tint(GamssTheme.colors.gray950),
            modifier = Modifier.size(width = LogoWidth, height = LogoHeight),
        )
        Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing200))
        Text(
            text = stringResource(R.string.login_subtitle),
            style = GamssTheme.typography.subtitle2,
            color = GamssTheme.colors.gray950,
        )
        Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing600))
        // 최소 여백을 두고 최대 폭을 제한하면, 디자인 기준 폭에서는 디자인과 같은 위치가 나오고
        // 좁은 화면에서는 여백이 유지되며 넓은 화면에서는 늘어나지 않는다.
        Image(
            painter = painterResource(R.drawable.img_login_characters),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = GamssTheme.spacing.spacing300)
                .widthIn(max = CharactersWidth)
                .fillMaxWidth()
                .aspectRatio(CharactersAspectRatio),
        )
        Spacer(modifier = Modifier.height(CharactersToButtonSpacing))
        GoogleSignInButton(
            isLoading = isLoading,
            onClick = onGoogleSignInClick,
            modifier = Modifier.padding(horizontal = GamssTheme.spacing.spacing300),
        )
        Spacer(modifier = Modifier.height(ButtonBottomSpacing))
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

// 세로 간격이 고정값이라 큰 글꼴과 좁은 화면이 레이아웃의 실제 한계다. 두 경우를 눈으로 확인한다.
@Preview(name = "Login - Font scale 2.0", showBackground = true, fontScale = 2.0f)
@Composable
@Suppress("UnusedPrivateMember")
private fun LoginContentLargeFontPreview() {
    GamssTheme {
        LoginContent(isLoading = false, onGoogleSignInClick = {})
    }
}

@Preview(name = "Login - Small screen", showBackground = true, widthDp = 320, heightDp = 560)
@Composable
@Suppress("UnusedPrivateMember")
private fun LoginContentSmallScreenPreview() {
    GamssTheme {
        LoginContent(isLoading = false, onGoogleSignInClick = {})
    }
}
