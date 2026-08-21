package com.gamss.android.feature.archive.navigation

import androidx.navigation3.runtime.NavKey
import com.gamss.android.domain.emotion.EmotionCharacter
import kotlinx.serialization.Serializable

/** enum을 직접 보존해 호출자가 임의의 문자열을 전달하여 상세 화면이 실패하는 일을 막는다. */
@Serializable
data class ArchiveDetailKey(val emotion: EmotionCharacter) : NavKey
