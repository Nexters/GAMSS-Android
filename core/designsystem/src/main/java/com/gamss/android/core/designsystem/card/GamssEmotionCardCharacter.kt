package com.gamss.android.core.designsystem.card

import androidx.annotation.DrawableRes
import com.gamss.android.core.designsystem.R

/** 감정 카드에 표시할 캐릭터 6종. 도메인 감정 모델은 화면 계층에서 이 타입으로 변환한다. */
enum class GamssEmotionCardCharacter(
    @DrawableRes internal val drawableRes: Int,
) {
    JOY(R.drawable.character_happy),
    ANGER(R.drawable.character_angry),
    ANXIETY(R.drawable.character_anxiety),
    SADNESS(R.drawable.character_sad),
    QUIRKY(R.drawable.character_wacky),
    PRICKLY(R.drawable.character_cranky),
}
