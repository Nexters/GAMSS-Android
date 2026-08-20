package com.gamss.android.core.common.util

import kotlin.math.roundToInt

/**
 * 오늘 사용한 토큰량을 오늘 하루 총 한도 대비 백분율로 환산한다.
 *
 * @param usedTokens 오늘 사용한 토큰 수.
 * @param dailyLimit 오늘 하루 총 한도. 서버가 "한도 없음"을 null 로 내려줄 수 있어 nullable 이다.
 * @return 0~100 사이로 clamp 된 정수 퍼센트. [dailyLimit] 이 null 이거나 0 이하라 나눌 수 없으면
 *  "0%"로 거짓 표시하지 않도록 null 을 반환한다 — 호출부가 무제한 상태를 구분해서 다뤄야 한다.
 */
fun calculateTokenUsagePercent(usedTokens: Long, dailyLimit: Long?): Int? {
    if (dailyLimit == null || dailyLimit <= 0) return null

    val percent = (usedTokens.toDouble() / dailyLimit.toDouble() * PERCENT_SCALE).roundToInt()
    return percent.coerceIn(0, 100)
}

private const val PERCENT_SCALE = 100
