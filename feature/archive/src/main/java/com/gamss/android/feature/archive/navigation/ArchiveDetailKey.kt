package com.gamss.android.feature.archive.navigation

import androidx.navigation3.runtime.NavKey
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.serialization.Serializable

/**
 * enum을 직접 보존해 호출자가 임의의 문자열을 전달하여 상세 화면이 실패하는 일을 막는다.
 *
 * @param droppedCardEpochDay 방금 버려서 이 화면으로 넘어온 카드의 날짜. 그 한 장만 떨어뜨리고
 *  나머지는 이미 쌓인 상태로 시작한다. 그냥 보관함에서 들어왔으면 null 이고, 이때는 전부 쏟는다.
 *  LocalDate 를 그대로 두면 직렬화가 안 되므로 epochDay 로 든다.
 */
@Serializable
data class ArchiveDetailKey(
    val emotion: EmotionCharacter,
    val droppedCardEpochDay: Long? = null,
) : NavKey
