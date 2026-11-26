package com.gamss.android.core.common.util

import kotlin.math.roundToInt

/**
 * 무료 한도 소진 후 유료 토큰으로 넘어간 사용량까지 하나의 막대에 이어서 보여주기 위한 퍼센트.
 *
 * [paidBalance]는 오늘 이미 차감된 뒤의 값이라 분모에 그대로 더하면, 유료 토큰을 쓸수록 분모가
 * 같이 줄어 소진 전에 퍼센트가 100%를 넘어버린다. 오늘 유료 지갑에서 빠져나간 몫
 * (`usedTokens - dailyLimit`)을 되돌려 더해 "오늘 기준 총 가용량"을 하루 동안 고정값으로
 * 재구성해야, 완전히 소진되는 시점에 정확히 100%로 맞아떨어진다.
 *
 * @return [dailyLimit] 이 null 이거나 0 이하면 null(무제한). 그 외엔 0~100 사이로 clamp 된 퍼센트.
 */
fun calculateCombinedTokenUsagePercent(usedTokens: Long, dailyLimit: Long?, paidBalance: Long?): Int? {
    if (dailyLimit == null || dailyLimit <= 0) return null

    val paidUsedToday = (usedTokens - dailyLimit).coerceAtLeast(0)
    val totalCapacity = (dailyLimit + paidUsedToday + (paidBalance ?: 0)).coerceAtLeast(dailyLimit)

    val percent = (usedTokens.toDouble() / totalCapacity.toDouble() * PERCENT_SCALE).roundToInt()
    return percent.coerceIn(0, 100)
}

private const val PERCENT_SCALE = 100
