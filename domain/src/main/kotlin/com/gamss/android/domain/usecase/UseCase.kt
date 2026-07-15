package com.gamss.android.domain.usecase

interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): R
}

interface NoParamUseCase<out R> {
    suspend operator fun invoke(): R
}
