package com.gamss.android.feature.chat.navigation

import androidx.navigation3.runtime.NavKey
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.serialization.Serializable

/**
 * @param initialMessage 새 대화([conversationId] 가 null)일 때만 자동으로 전송한다.
 * @param excludeCharacterNames 홈에서 체크를 해제한 캐릭터의 enum 이름. 새 대화를 열 때만 서버에 실린다.
 *  enum 을 그대로 담지 않는다. NavKey 는 androidx savedstate 로 저장되고 enum 은 ordinal 로 직렬화되어,
 *  [EmotionCharacter] 상수 순서가 바뀌면 복원된 백스택이 조용히 다른 캐릭터를 가리킨다.
 */
@Serializable
data class ChatRoomKey(
    val conversationId: Long? = null,
    val initialMessage: String? = null,
    val excludeCharacterNames: Set<String> = emptySet(),
) : NavKey

fun Set<EmotionCharacter>.toKeyNames(): Set<String> = mapTo(mutableSetOf()) { it.name }

/** 저장된 이름이 더 이상 없는 상수라면 버린다. 제외 목록이 줄어드는 쪽이 대화를 못 여는 쪽보다 안전하다. */
fun Set<String>.toEmotionCharacters(): Set<EmotionCharacter> =
    mapNotNullTo(mutableSetOf()) { name -> EmotionCharacter.entries.firstOrNull { it.name == name } }
