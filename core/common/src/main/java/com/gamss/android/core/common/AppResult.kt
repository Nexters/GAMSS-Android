package com.gamss.android.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val throwable: Throwable) : AppResult<Nothing>

    companion object {
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

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data
