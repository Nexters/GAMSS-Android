package com.gamss.android.feature.archive.navigation

import androidx.navigation3.runtime.NavKey
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.serialization.Serializable

/**
 * enum을 직접 보존해 호출자가 임의의 문자열을 전달하여 상세 화면이 실패하는 일을 막는다.
 *
 * 어느 감정 칸인지만 든다. key 는 data class 라 값이 곧 화면의 정체성이어서, 방금 버린 카드
 * 같은 일회성 신호를 얹으면 같은 화면이 인자만 다른 두 개의 key 로 존재한다. 그 신호는
 * Navigator 가 따로 들고 화면이 한 번 받아 간다.
 */
@Serializable
data class ArchiveDetailKey(val emotion: EmotionCharacter) : NavKey
