package com.gamss.android.domain

import com.gamss.android.core.common.AppResult

internal fun <T> AppResult<T>.assertSuccess(): T = when (this) {
    is AppResult.Success -> data
    is AppResult.Failure -> throw AssertionError("Success 를 기대했지만 Failure: $throwable", throwable)
}
