package com.gamss.android.data.remote.token.model.response

import com.gamss.android.domain.model.DailyTokenUsage
import kotlinx.serialization.Serializable

@Serializable
internal data class DailyTokenUsageDataResponse(
    val usedTokens: Long,
    // 한도가 없는 계정은 서버가 키를 아예 내려주지 않는다.
    val dailyLimit: Long? = null,
    val exceeded: Boolean,
) {
    fun toDomain() = DailyTokenUsage(
        usedTokens = usedTokens,
        dailyLimit = dailyLimit,
        exceeded = exceeded,
    )
}
