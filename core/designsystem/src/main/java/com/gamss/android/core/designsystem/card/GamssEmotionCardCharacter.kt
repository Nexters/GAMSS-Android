package com.gamss.android.core.designsystem.card

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.gamss.android.core.designsystem.R

/** 감정 카드에 표시할 캐릭터 6종. 도메인 감정 모델은 화면 계층에서 이 타입으로 변환한다. */
enum class GamssEmotionCardCharacter(
    @get:DrawableRes internal val drawableRes: Int,
) {
    JOY(R.drawable.character_happy),
    ANGER(R.drawable.character_angry),
    ANXIETY(R.drawable.character_anxiety),
    SADNESS(R.drawable.character_sad),
    QUIRKY(R.drawable.character_wacky),
    PRICKLY(R.drawable.character_cranky),
}

/** 감정 카드 캐릭터를 원본 270×156 비율로 표시한다. */
@Composable
fun GamssEmotionCardCharacterImage(
    character: GamssEmotionCardCharacter,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(character.drawableRes),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.fillMaxSize(),
    )
}
