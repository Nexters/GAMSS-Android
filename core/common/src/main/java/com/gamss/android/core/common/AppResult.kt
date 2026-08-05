package com.gamss.android.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val throwable: Throwable) : AppResult<Nothing>

    companion object {
        @Suppress("TooGenericExceptionCaught")
        inline fun <T> of(block: () -> T): AppResult<T> =
            try {
                Success(block())
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

inline fun <T> AppResult<T>.mapFailure(
    transform: (Throwable) -> Throwable,
): AppResult<T> = when (this) {
    is AppResult.Success -> this
    is AppResult.Failure -> AppResult.Failure(transform(throwable))
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
