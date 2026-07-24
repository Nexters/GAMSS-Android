package com.gamss.android.domain.summary

import javax.inject.Inject

/**
 * USER 발화 목록 → 카드/콜백용 압축 요약 문장.
 *
 * 발화를 공백으로 이어 한 덩어리로 요약한다. 이 요약은 (1) 카드에 누적 저장되고
 * (2) 서버가 대화를 생성할 때 전체 히스토리를 매번 재요약하지 않도록 컨텍스트 압축본으로 재사용된다.
 * 감정 분류와 독립적으로 원문에서 바로 생성한다(요약→감정 체이닝의 신호 손실 회피).
 * 발화가 없거나 모두 공백이면 요약할 대상이 없어 null.
 */
class SummarizeDiaryUseCase @Inject constructor(
    private val summarizer: DiarySummarizer,
) {
    suspend operator fun invoke(userUtterances: List<String>): String? {
        val text = userUtterances.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = " ")
        return if (text.isEmpty()) null else summarizer.summarize(text)
    }
}
