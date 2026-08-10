package com.gamss.android.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class AppResultTest {

    @Test
    fun 정상_반환은_성공으로_감싼다() {
        val result = AppResult.of { 1 }

        assertEquals(AppResult.Success(1), result)
    }

    @Test
    fun 일반_예외는_실패로_감싼다() {
        val result = AppResult.of { error("boom") }

        assertTrue(result is AppResult.Failure)
    }

    @Test(expected = CancellationException::class)
    fun 취소는_삼키지_않고_다시_던진다() {
        AppResult.of { throw CancellationException("cancelled") }
    }
}
