package com.gamss.android.feature.onboarding

import com.gamss.android.domain.analytics.AnalyticsEvent
import com.gamss.android.domain.analytics.AnalyticsProperty
import com.gamss.android.domain.analytics.AnalyticsTracker
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingViewModelTest {

    private val analyticsTracker = RecordingAnalyticsTracker()
    private val viewModel = OnboardingViewModel(analyticsTracker)

    @Test
    fun `다음을 누르면 보던 페이지 번호를 1부터 세어 보낸다`() {
        viewModel.trackNext(currentPage = 0)
        viewModel.trackNext(currentPage = 1)

        assertEquals(
            listOf(
                AnalyticsEvent.ONBOARDING_NEXT to mapOf(AnalyticsProperty.STEP to 1),
                AnalyticsEvent.ONBOARDING_NEXT to mapOf(AnalyticsProperty.STEP to 2),
            ),
            analyticsTracker.events,
        )
    }

    @Test
    fun `건너뛰기를 누르면 보던 페이지 번호를 1부터 세어 보낸다`() {
        viewModel.trackSkip(currentPage = 1)

        assertEquals(
            listOf(AnalyticsEvent.ONBOARDING_SKIP to mapOf(AnalyticsProperty.STEP to 2)),
            analyticsTracker.events,
        )
    }

    @Test
    fun `온보딩을 마치면 속성 없이 완료 이벤트를 보낸다`() {
        viewModel.trackComplete()

        assertEquals(
            listOf(AnalyticsEvent.ONBOARDING_COMPLETE to emptyMap<String, Any>()),
            analyticsTracker.events,
        )
    }
}

private class RecordingAnalyticsTracker : AnalyticsTracker {
    val events = mutableListOf<Pair<String, Map<String, Any>>>()

    override fun track(event: String, properties: Map<String, Any>) {
        events += event to properties
    }
}
