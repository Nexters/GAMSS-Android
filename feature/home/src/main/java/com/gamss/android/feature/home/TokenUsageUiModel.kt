package com.gamss.android.feature.home

import com.gamss.android.domain.model.DailyTokenUsage
import java.util.Locale
import kotlin.math.roundToInt

data class TokenUsageUiModel(
    val usedTokens: Long,
    /** 0 이하 한도는 무제한으로 보아 null 로 정규화한 값만 담는다. */
    val dailyLimit: Long?,
    val exceeded: Boolean,
) {
    val usageRatio: Float? get() = dailyLimit?.let { usedTokens.toFloat() / it }
}

internal fun DailyTokenUsage.toUiModel() = TokenUsageUiModel(
    usedTokens = usedTokens,
    dailyLimit = dailyLimit?.takeIf { it > 0 },
    exceeded = exceeded,
)

internal data class TokenUsageDisplayText(
    val headline: String,
    val supportingText: String?,
)

internal fun TokenUsageUiModel.toDisplayText(locale: Locale = Locale.getDefault()): TokenUsageDisplayText {
    val absoluteText = dailyLimit
        ?.let { "${usedTokens.formatTokenCount(locale)} / ${it.formatTokenCount(locale)}" }
        ?: usedTokens.formatTokenCount(locale)

    return TokenUsageDisplayText(
        headline = usageRatio?.let { "${(it * 100).roundToInt()}%" } ?: absoluteText,
        supportingText = absoluteText.takeIf { usageRatio != null },
    )
}

private fun Long.formatTokenCount(locale: Locale): String = String.format(locale, "%,d", this)
