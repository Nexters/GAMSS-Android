package com.gamss.android.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 구글 로그인을 계측으로 통과할 수 없어 로그인 화면까지만 수집한다. 홈 이후는 별도 시나리오가 필요하다. */
@RunWith(AndroidJUnit4::class)
class StartupBaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
    }

    private companion object {
        /** release 계열 variant 에서만 수집한다. debug 의 `.dev` suffix 는 붙지 않는다. */
        const val TARGET_PACKAGE = "com.gamss.android"
    }
}
