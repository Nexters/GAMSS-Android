package com.gamss.android.feature.archive.navigation

import androidx.navigation3.runtime.NavKey
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.serialization.Serializable

/**
 * enum을 직접 보존해 호출자가 임의의 문자열을 전달하여 상세 화면이 실패하는 일을 막는다.
 *
 * key 는 data class 라 값이 곧 화면의 정체성이다. 방금 버린 카드 같은 일회성 신호를 얹으면 같은
 * 화면이 인자만 다른 두 개의 key 로 존재하므로, 그런 신호는 Navigator 가 따로 든다.
 */
@Serializable
data class ArchiveDetailKey(val emotion: EmotionCharacter) : NavKey
