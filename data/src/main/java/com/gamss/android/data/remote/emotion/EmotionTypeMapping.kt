package com.gamss.android.data.remote.emotion

import android.util.Log
import com.gamss.android.domain.emotion.EmotionCharacter

/** 서버 `emotionType` 문자열에 기대는 코드는 이 파일에만 둔다. 카드 생성은 역방향을 쓴다. */
private val serverTypeToCharacter = mapOf(
    "JOY" to EmotionCharacter.JOY,
    "ANGER" to EmotionCharacter.ANGER,
    "ANXIETY" to EmotionCharacter.ANXIETY,
    "GRUMPY" to EmotionCharacter.PRICKLY,
    "WARM" to EmotionCharacter.WARM,
    "QUIRKY" to EmotionCharacter.QUIRKY,
)

private val characterToServerType = serverTypeToCharacter.entries.associate { (type, character) ->
    character to type
}

internal fun String?.toEmotionCharacter(): EmotionCharacter? =
    serverTypeToCharacter[this] ?: run {
        Log.w(TAG, "Unknown emotionType=$this")
        null
    }

internal fun EmotionCharacter.toServerEmotionType(): String =
    characterToServerType.getValue(this)

private const val TAG = "EmotionTypeMapping"
