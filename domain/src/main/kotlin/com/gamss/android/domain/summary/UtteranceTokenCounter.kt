package com.gamss.android.domain.summary

interface UtteranceTokenCounter {
    suspend fun count(text: String): Int

    /** 복원처럼 여러 발화를 한꺼번에 셀 때 디스패치를 한 번만 태운다. */
    suspend fun countAll(texts: List<String>): List<Int> = texts.map { count(it) }
}
