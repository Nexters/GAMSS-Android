package com.gamss.android.core.common

import kotlin.coroutines.cancellation.CancellationException

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val throwable: Throwable) : AppResult<Nothing>

    companion object {
        @Suppress("TooGenericExceptionCaught", "RethrowCaughtException")
        inline fun <T> of(block: () -> T): AppResult<T> =
            try {
                Success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Failure(t)
            }
    }
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Failure -> this
    }

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
