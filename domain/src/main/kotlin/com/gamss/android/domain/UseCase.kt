package com.gamss.android.domain

interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): R
}

interface NoParamUseCase<out R> {
    suspend operator fun invoke(): R
}
