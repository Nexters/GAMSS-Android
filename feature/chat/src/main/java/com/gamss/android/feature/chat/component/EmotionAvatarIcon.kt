package com.gamss.android.feature.chat.component

import androidx.annotation.DrawableRes
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.core.designsystem.R as DesignSystemR

/** [EmotionCharacter] 6종 각각에 대응하는 core:designsystem 아바타 아이콘. */
internal val EmotionCharacter.avatarIconRes: Int
    @DrawableRes get() = when (this) {
        EmotionCharacter.JOY -> DesignSystemR.drawable.ic_avatar_joy
        EmotionCharacter.ANGER -> DesignSystemR.drawable.ic_avatar_anger
        EmotionCharacter.ANXIETY -> DesignSystemR.drawable.ic_avatar_anxiety
        EmotionCharacter.SADNESS -> DesignSystemR.drawable.ic_avatar_sadness
        EmotionCharacter.QUIRKY -> DesignSystemR.drawable.ic_avatar_quirky
        EmotionCharacter.PRICKLY -> DesignSystemR.drawable.ic_avatar_prickly
    }
