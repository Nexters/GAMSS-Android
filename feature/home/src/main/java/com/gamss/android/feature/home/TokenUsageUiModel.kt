package com.gamss.android.feature.home

import com.gamss.android.domain.model.DailyTokenUsage
import java.util.Locale
import kotlin.math.roundToInt

data class TokenUsageUiModel(
    val usedTokens: Long,
    val dailyLimit: Long?,
    val usageRatio: Float?,
    val exceeded: Boolean,
    val displayMode: TokenUsageDisplayMode,
)

enum class TokenUsageDisplayMode {
    PERCENT,
    ABSOLUTE,
    PERCENT_WITH_ABSOLUTE,
}

internal fun DailyTokenUsage.toUiModel(
    displayMode: TokenUsageDisplayMode,
): TokenUsageUiModel {
    val effectiveDailyLimit = dailyLimit?.takeIf { it > 0 }
    val usageRatio = effectiveDailyLimit?.let { usedTokens.toFloat() / it.toFloat() }

    return TokenUsageUiModel(
        usedTokens = usedTokens,
        dailyLimit = effectiveDailyLimit,
        usageRatio = usageRatio,
        exceeded = exceeded,
        displayMode = displayMode,
    )
}

internal data class TokenUsageDisplayText(
    val headline: String,
    val supportingText: String?,
)

internal fun TokenUsageUiModel.toDisplayText(locale: Locale = Locale.getDefault()): TokenUsageDisplayText {
    val absoluteText = dailyLimit?.let {
        "${usedTokens.formatTokenCount(locale)} / ${it.formatTokenCount(locale)}"
    } ?: usedTokens.formatTokenCount(locale)
    val percentText = usageRatio?.let { "${(it * 100).roundToInt()}%" }

    return when (displayMode) {
        TokenUsageDisplayMode.PERCENT -> TokenUsageDisplayText(
            headline = percentText ?: absoluteText,
            supportingText = dailyLimit?.let { absoluteText },
        )
        TokenUsageDisplayMode.ABSOLUTE -> TokenUsageDisplayText(absoluteText, null)
        TokenUsageDisplayMode.PERCENT_WITH_ABSOLUTE -> TokenUsageDisplayText(
            headline = percentText ?: absoluteText,
            supportingText = dailyLimit?.let { absoluteText },
        )
    }
}

private fun Long.formatTokenCount(locale: Locale): String = String.format(locale, "%,d", this)
