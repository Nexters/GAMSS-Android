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

    // STEP 은 페이저 인덱스가 아니라 사용자가 보던 페이지 번호(1부터)로 보낸다.
    fun trackNext(currentPage: Int) {
        analyticsTracker.track(AnalyticsEvent.ONBOARDING_NEXT, mapOf(AnalyticsProperty.STEP to currentPage + 1))
    }

    fun trackSkip(currentPage: Int) {
        analyticsTracker.track(AnalyticsEvent.ONBOARDING_SKIP, mapOf(AnalyticsProperty.STEP to currentPage + 1))
    }

    fun trackComplete() {
        analyticsTracker.track(AnalyticsEvent.ONBOARDING_COMPLETE, emptyMap())
    }
}
