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
        ).toUiModel()

        assertEquals(12_000L, uiModel.usedTokens)
        assertEquals(100_000L, uiModel.dailyLimit)
        assertEquals(0.12f, requireNotNull(uiModel.usageRatio), 0.0001f)
        assertFalse(uiModel.exceeded)
    }

    @Test
    fun `keeps exceeded ratio for the presentation layer while progress can be capped`() {
        val uiModel = DailyTokenUsage(
            usedTokens = 125_000,
            dailyLimit = 100_000,
            exceeded = true,
        ).toUiModel()

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
            ).toUiModel()

            assertNull(uiModel.dailyLimit)
            assertNull(uiModel.usageRatio)
        }
    }

    @Test
    fun `formatter shows percent with the absolute count and falls back when unlimited`() {
        val finite = DailyTokenUsage(usedTokens = 12_000, dailyLimit = 100_000, exceeded = false)
            .toUiModel()
            .toDisplayText(Locale.US)
        val unlimited = DailyTokenUsage(usedTokens = 12_000, dailyLimit = null, exceeded = false)
            .toUiModel()
            .toDisplayText(Locale.US)

        assertEquals(TokenUsageDisplayText("12%", "12,000 / 100,000"), finite)
        assertEquals(TokenUsageDisplayText("12,000", null), unlimited)
    }

    @Test
    fun `formatter preserves exceeded percent above one hundred`() {
        val displayText = DailyTokenUsage(125_000, 100_000, true)
            .toUiModel()
            .toDisplayText(Locale.US)

        assertEquals(TokenUsageDisplayText("125%", "125,000 / 100,000"), displayText)
    }
}
