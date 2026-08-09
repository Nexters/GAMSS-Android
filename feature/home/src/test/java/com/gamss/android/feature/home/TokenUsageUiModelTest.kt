package com.gamss.android.feature.home

import com.gamss.android.domain.model.DailyTokenUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class TokenUsageUiModelTest {

    @Test
    fun `maps finite daily usage without formatting presentation strings`() {
        val uiModel = DailyTokenUsage(
            usedTokens = 12_000,
            dailyLimit = 100_000,
            exceeded = false,
        ).toUiModel(TokenUsageDisplayMode.PERCENT_WITH_ABSOLUTE)

        assertEquals(12_000L, uiModel.usedTokens)
        assertEquals(100_000L, uiModel.dailyLimit)
        assertEquals(0.12f, requireNotNull(uiModel.usageRatio), 0.0001f)
        assertEquals(TokenUsageDisplayMode.PERCENT_WITH_ABSOLUTE, uiModel.displayMode)
        assertFalse(uiModel.exceeded)
    }

    @Test
    fun `keeps exceeded ratio for the presentation layer while progress can be capped`() {
        val uiModel = DailyTokenUsage(
            usedTokens = 125_000,
            dailyLimit = 100_000,
            exceeded = true,
        ).toUiModel(TokenUsageDisplayMode.PERCENT)

        val usageRatio = requireNotNull(uiModel.usageRatio)

        assertEquals(1.25f, usageRatio, 0.0001f)
        assertEquals(1f, usageRatio.coerceIn(0f, 1f), 0.0001f)
    }

    @Test
    fun `unlimited or zero daily limit omits ratio`() {
        listOf<Long?>(null, 0).forEach { dailyLimit ->
            val uiModel = DailyTokenUsage(
                usedTokens = 12_000,
                dailyLimit = dailyLimit,
                exceeded = false,
            ).toUiModel(TokenUsageDisplayMode.ABSOLUTE)

            assertNull(uiModel.dailyLimit)
            assertNull(uiModel.usageRatio)
        }
    }

    @Test
    fun `formatter supports all display modes and unlimited fallback`() {
        val finiteUsage = DailyTokenUsage(
            usedTokens = 12_000,
            dailyLimit = 100_000,
            exceeded = false,
        )

        val percent = finiteUsage.toUiModel(TokenUsageDisplayMode.PERCENT).toDisplayText(Locale.US)
        val absolute = finiteUsage.toUiModel(TokenUsageDisplayMode.ABSOLUTE).toDisplayText(Locale.US)
        val combined = finiteUsage.toUiModel(TokenUsageDisplayMode.PERCENT_WITH_ABSOLUTE)
            .toDisplayText(Locale.US)
        val unlimited = DailyTokenUsage(12_000, null, false)
            .toUiModel(TokenUsageDisplayMode.PERCENT)
            .toDisplayText(Locale.US)

        assertEquals(TokenUsageDisplayText("12%", "12,000 / 100,000"), percent)
        assertEquals(TokenUsageDisplayText("12,000 / 100,000", null), absolute)
        assertEquals(TokenUsageDisplayText("12%", "12,000 / 100,000"), combined)
        assertEquals(TokenUsageDisplayText("12,000", null), unlimited)
    }

    @Test
    fun `formatter preserves exceeded percent above one hundred`() {
        val displayText = DailyTokenUsage(125_000, 100_000, true)
            .toUiModel(TokenUsageDisplayMode.PERCENT)
            .toDisplayText(Locale.US)

        assertEquals(TokenUsageDisplayText("125%", "125,000 / 100,000"), displayText)
    }
}
