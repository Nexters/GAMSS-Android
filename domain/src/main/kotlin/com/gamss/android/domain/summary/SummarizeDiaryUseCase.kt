package com.gamss.android.domain.summary

import com.gamss.android.core.common.AppResult
import com.gamss.android.domain.usecase.UseCase
import javax.inject.Inject

/**
 * USER 발화 목록 → 카드/콜백용 압축 요약 문장.
 *
 * 발화를 공백으로 이어 한 덩어리로 요약한다. 이 요약은 (1) 카드에 누적 저장되고
 * (2) 서버가 대화를 생성할 때 전체 히스토리를 매번 재요약하지 않도록 컨텍스트 압축본으로 재사용된다.
 * 감정 분류와 독립적으로 원문에서 바로 생성한다(요약→감정 체이닝의 신호 손실 회피).
 */
class SummarizeDiaryUseCase @Inject constructor(
    private val summarizer: DiarySummarizer,
) : UseCase<List<String>, AppResult<String?>> {

    override suspend fun invoke(params: List<String>): AppResult<String?> = AppResult.of {
        val text = params.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = " ")
        // 이미 충분히 짧으면 요약할 게 없고, 짧은 캐주얼 입력은 요약 모델(긴 문서 학습)의 분포 밖이라
        // 반복/할루시네이션을 유발한다. 이 경우 요약기를 호출하지 않고 원문을 그대로 쓴다.
        when {
            text.isEmpty() -> null
            text.length < MIN_CHARS_FOR_SUMMARY -> text
            else -> summarizer.summarize(text)
        }
    }

    private companion object {
        const val MIN_CHARS_FOR_SUMMARY = 50
    }
}
