package com.gamss.android.feature.onboarding

import androidx.lifecycle.ViewModel
import com.gamss.android.domain.analytics.AnalyticsEvent
import com.gamss.android.domain.analytics.AnalyticsProperty
import com.gamss.android.domain.analytics.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    fun trackNext(pageIndex: Int) {
        analyticsTracker.track(AnalyticsEvent.ONBOARDING_NEXT, mapOf(AnalyticsProperty.STEP to pageIndex + 1))
    }

    fun trackSkip(pageIndex: Int) {
        analyticsTracker.track(AnalyticsEvent.ONBOARDING_SKIP, mapOf(AnalyticsProperty.STEP to pageIndex + 1))
    }

    fun trackComplete() {
        analyticsTracker.track(AnalyticsEvent.ONBOARDING_COMPLETE)
    }
}
