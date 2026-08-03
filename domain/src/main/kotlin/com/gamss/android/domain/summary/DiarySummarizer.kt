package com.gamss.android.domain.summary

/** 텍스트 → 압축 요약 문장. 온디바이스 구현이 이 포트를 만족한다. */
interface DiarySummarizer {
    suspend fun summarize(text: String): String
}
