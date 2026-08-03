package com.gamss.android.domain.summary

/** 요약기 입력 한계를 재기 위한 토큰 카운터. 절단 없이 실제 토큰 수를 돌려줘야 한다. */
interface UtteranceTokenCounter {
    suspend fun count(text: String): Int
}
