package com.gamss.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun of_capturesSuccess() {
        val result = AppResult.of { 42 }
        assertTrue(result is AppResult.Success)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun of_capturesFailure() {
        val result = AppResult.of { error("boom") }
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun map_transformsSuccess() {
        val result = AppResult.Success(2).map { it * 3 }
        assertEquals(6, result.getOrNull())
    }

    @Test
    fun map_passesThroughFailure() {
        val failure: AppResult<Int> = AppResult.Failure(IllegalStateException())
        val mapped = failure.map { it * 3 }
        assertTrue(mapped is AppResult.Failure)
    }
}
