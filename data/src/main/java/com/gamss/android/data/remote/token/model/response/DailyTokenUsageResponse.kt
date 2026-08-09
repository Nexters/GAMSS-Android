package com.gamss.android.data.remote.token.model.response

import com.gamss.android.domain.model.DailyTokenUsage
import kotlinx.serialization.Serializable

@Serializable
internal data class DailyTokenUsageDataResponse(
    val usedTokens: Long,
    val dailyLimit: Long?,
    val exceeded: Boolean,
) {
    fun toDomain() = DailyTokenUsage(
        usedTokens = usedTokens,
        dailyLimit = dailyLimit,
        exceeded = exceeded,
    )
}
