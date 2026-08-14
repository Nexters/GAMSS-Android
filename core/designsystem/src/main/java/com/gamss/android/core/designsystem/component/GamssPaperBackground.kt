package com.gamss.android.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.gamss.android.core.designsystem.R
import com.gamss.android.core.designsystem.theme.GamssTheme

/** 장식을 절대 좌표로 얹는 화면이 있어 [clipToBounds] 로 넘치는 부분이 레이아웃을 밀지 않게 막는다. */
@Composable
fun GamssPaperBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GamssTheme.colors.gray025)
            .clipToBounds(),
    ) {
        Image(
            painter = painterResource(R.drawable.bg_paper_texture),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        content()
    }
}
