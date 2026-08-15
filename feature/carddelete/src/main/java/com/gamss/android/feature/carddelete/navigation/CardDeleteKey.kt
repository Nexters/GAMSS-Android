package com.gamss.android.feature.carddelete.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 삭제 대상은 화면 밖에서 선택하고, 이 키에는 안정적인 식별자만 전달한다. */
@Serializable
data class CardDeleteKey(
    val cardId: Long,
    val isAnimationPreview: Boolean = false,
) : NavKey
