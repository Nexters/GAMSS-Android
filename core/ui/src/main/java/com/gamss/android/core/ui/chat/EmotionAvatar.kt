package com.gamss.android.core.ui.chat

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.core.designsystem.R as DesignSystemR

/** [EmotionCharacter] 6종 각각에 대응하는 core:designsystem 아바타 아이콘. */
val EmotionCharacter.avatarIconRes: Int
    @DrawableRes get() = when (this) {
        EmotionCharacter.JOY -> DesignSystemR.drawable.ic_avatar_joy
        EmotionCharacter.ANGER -> DesignSystemR.drawable.ic_avatar_anger
        EmotionCharacter.ANXIETY -> DesignSystemR.drawable.ic_avatar_anxiety
        EmotionCharacter.SADNESS -> DesignSystemR.drawable.ic_avatar_sadness
        EmotionCharacter.QUIRKY -> DesignSystemR.drawable.ic_avatar_quirky
        EmotionCharacter.PRICKLY -> DesignSystemR.drawable.ic_avatar_prickly
    }

/** 원형 클립과 테두리는 아바타 자리가 맡으므로 여기서는 그 자리를 꽉 채우기만 한다. */
@Composable
fun CharacterAvatar(character: EmotionCharacter, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(character.avatarIconRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}
