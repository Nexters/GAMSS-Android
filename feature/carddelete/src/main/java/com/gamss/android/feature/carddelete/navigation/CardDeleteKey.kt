package com.gamss.android.feature.carddelete.navigation

import androidx.navigation3.runtime.NavKey
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.serialization.Serializable

/**
 * 삭제 대상은 화면 밖에서 고르고, 이 키에는 안정적인 식별자만 전달한다.
 *
 * @param emotion 파쇄 대상이 속한 감정 칸. 삭제는 늘 한 칸 안에서 일어나므로 범위를 벗어나지
 *  않는다.
 * @param cardId 파쇄할 카드. null 이면 [emotion] 칸을 통째로 파쇄한다. 무엇을 지우는지가 이
 *  화면의 정체성이라 키에 담는다: 같은 카드로 다시 들어오면 같은 화면이어야 하고, 다른 카드면
 *  다른 화면이어야 한다.
 */
@Serializable
data class CardDeleteKey(
    val emotion: EmotionCharacter,
    val cardId: Long? = null,
) : NavKey
