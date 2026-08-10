package com.gamss.android.domain.summary

interface UtteranceTokenCounter {
    suspend fun count(text: String): Int
}
