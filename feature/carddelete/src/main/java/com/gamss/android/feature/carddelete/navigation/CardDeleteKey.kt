package com.gamss.android.feature.carddelete.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 삭제 대상은 화면 밖에서 고르고, 이 키에는 안정적인 식별자만 넘긴다.
 *
 * [cardId] 가 있으면 그 카드 한 장만, null 이면 가진 카드를 전부 파쇄한다.
 */
@Serializable
data class CardDeleteKey(val cardId: Long? = null) : NavKey
