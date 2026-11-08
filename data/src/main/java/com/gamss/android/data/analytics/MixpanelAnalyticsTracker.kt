package com.gamss.android.data.analytics

import com.gamss.android.domain.analytics.AnalyticsTracker
import com.mixpanel.android.mpmetrics.MixpanelAPI

internal class MixpanelAnalyticsTracker(
    private val mixpanel: MixpanelAPI,
) : AnalyticsTracker {

    override fun track(event: String, properties: Map<String, Any>) {
        mixpanel.trackMap(event, properties)
    }
}
