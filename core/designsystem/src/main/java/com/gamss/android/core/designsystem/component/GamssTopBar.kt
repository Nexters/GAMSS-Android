package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gamss.android.core.designsystem.R

private val TopBarHeight = 62.dp

/**
 * 가운데 슬롯이 없다. 타이틀이 필요한 화면은 [leading] 안에서 직접 배치한다.
 *
 * 화면이 edge-to-edge 라 상태바 영역은 이 컴포넌트가 직접 피한다.
 */
@Composable
fun GamssTopBar(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(TopBarHeight)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, content = leading)
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

/** GAMSS 워드마크. 크기는 VectorDrawable 에 박힌 값(디자인 70x20 + stroke 여백)을 그대로 쓴다. */
@Composable
fun GamssLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(R.drawable.ic_gamss_logo),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
