package com.gamss.android.feature.login.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.theme.GamssTheme
import com.gamss.android.core.designsystem.theme.LightGamssColors
import com.gamss.android.feature.login.R

// 구글 버튼은 브랜드 가이드에 따라 라이트/다크에서 같은 색을 유지한다.
// 그래서 반전하는 테마 토큰이 아니라 라이트 팔레트를 직접 참조한다.
private val ButtonContainer = LightGamssColors.gray050
private val ButtonBorder = LightGamssColors.gray300

// Figma 의 Gray/Gray1000. 라이트/다크 공통 값이라 반전하는 GamssColors 에는 자리가 없다.
private val ButtonContent = Color(0xFF1A1C20)

private val GoogleMarkSize = 36.dp

@Composable
internal fun GoogleSignInButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(GamssTheme.radius.radius200),
        colors = ButtonDefaults.buttonColors(
            containerColor = ButtonContainer,
            contentColor = ButtonContent,
            disabledContainerColor = ButtonContainer,
            disabledContentColor = ButtonContent,
        ),
        border = BorderStroke(width = 1.dp, color = ButtonBorder),
    ) {
        // 로딩 중에도 라벨을 남긴다. Button 이 자식 시맨틱을 병합하므로 라벨을 빼면 접근성 이름이 사라진다.
        Box(
            modifier = Modifier.size(GoogleMarkSize),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = ButtonContent,
                    strokeWidth = 2.dp,
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_google_logo),
                    contentDescription = null,
                    modifier = Modifier.size(GoogleMarkSize),
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.login_google_sign_in),
            style = GamssTheme.typography.subtitle3,
        )
    }
}
