package com.gamss.android.feature.login.component

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.theme.LightGamssColors
import com.gamss.android.feature.login.R

// Figma 로그인 프레임의 고유 치수라 GamssSpacing 스케일에 없다.
private val ButtonMinHeight = 54.dp
private val ButtonMaxWidth = 366.dp
private val BorderWidth = 1.dp
private val GoogleMarkSize = 36.dp
private val ProgressSize = 24.dp
private val ProgressStrokeWidth = 2.dp

@Composable
internal fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        // 높이를 고정하면 큰 글꼴 배율에서 라벨이 잘린다. 최소 높이만 잡고 늘어나게 둔다.
        modifier = modifier
            .widthIn(max = ButtonMaxWidth)
            .fillMaxWidth()
            .heightIn(min = ButtonMinHeight),
        shape = RoundedCornerShape(GamssTheme.radius.radius200),
        // 디자인이 다크 프레임에서도 라이트 팔레트 토큰을 지정한다. 그래서 테마를 따르지 않고 라이트로 고정한다.
        // 비활성 색을 활성 색과 같게 둔 것은 비활성 경로가 로딩뿐이고, 그때는 인디케이터가 상태를 알리기 때문이다.
        colors = ButtonDefaults.buttonColors(
            containerColor = LightGamssColors.gray050,
            contentColor = GamssTheme.colors.gray1000,
            disabledContainerColor = LightGamssColors.gray050,
            disabledContentColor = GamssTheme.colors.gray1000,
        ),
        border = BorderStroke(width = BorderWidth, color = LightGamssColors.gray300),
    ) {
        // 스피너와 구글 마크가 같은 자리를 차지해야 로딩 전환 시 라벨이 밀리지 않는다.
        Box(
            modifier = Modifier.size(GoogleMarkSize),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ProgressSize),
                    color = GamssTheme.colors.gray1000,
                    strokeWidth = ProgressStrokeWidth,
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_google_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(modifier = Modifier.width(GamssTheme.spacing.spacing150))
        // Button 이 자식 시맨틱을 병합하므로, 로딩 중에도 이 라벨이 있어야 접근성 이름이 유지된다.
        Text(
            text = stringResource(R.string.login_google_sign_in),
            style = GamssTheme.typography.subtitle3,
        )
    }
}

// 이 버튼은 다크에서도 라이트 팔레트를 쓰는 유일한 지점이라 두 테마를 함께 확인한다.
@Preview(name = "GoogleSignInButton - Light", showBackground = true)
@Preview(
    name = "GoogleSignInButton - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
@Suppress("UnusedPrivateMember")
private fun GoogleSignInButtonPreview() {
    GamssTheme {
        Column(
            modifier = Modifier
                .background(GamssTheme.colors.background)
                .padding(GamssTheme.spacing.spacing300),
        ) {
            GoogleSignInButton(isLoading = false, onClick = {})
            Spacer(modifier = Modifier.height(GamssTheme.spacing.spacing300))
            GoogleSignInButton(isLoading = true, onClick = {})
        }
    }
}
