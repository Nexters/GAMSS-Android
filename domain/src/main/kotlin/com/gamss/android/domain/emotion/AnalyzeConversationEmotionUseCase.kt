package com.gamss.android.domain.emotion

import javax.inject.Inject

/** 대화에서 뽑은 메인 화자와 그 화자의 대표 감정. */
data class ConversationEmotion(
    val mainSpeaker: String,
    val emotion: ClassificationResult,
)

/**
 * 카톡형 대화 텍스트 → 메인 화자 식별 → 그 화자의 발화를 각각 분류해 대표 감정을 고른다.
 *
 * 발화를 통째로 이어붙이지 않고 발화별로 분류하는 이유:
 * - 모델이 단문으로 학습돼 발화 단위 입력이 학습 분포와 정합한다.
 * - 긴 대화를 이어붙이면 128토큰에서 뒤가 잘려 최근 맥락이 사라진다.
 * 집계는 "최고 신뢰" 발화를 대표로 삼는다(동률이면 더 최근 발화). 다수결은
 * 잡담성 저신뢰 발화가 표를 갈라 오답을 내므로 쓰지 않는다.
 * 화자 접두가 없는 일기 단문은 발화가 하나라 종전과 동일하게 동작한다.
 */
class AnalyzeConversationEmotionUseCase @Inject constructor(
    private val classifier: EmotionClassifier,
) {
    suspend operator fun invoke(rawConversation: String): ConversationEmotion? {
        val turns = ConversationParser.parse(rawConversation)
        val speaker = turns.mainSpeaker() ?: return null
        val messages = turns.asSequence()
            .filter { it.speaker == speaker }
            .map { it.message.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        // index 를 tiebreak 로 써서 신뢰도가 같으면 더 최근(뒤) 발화를 택한다.
        val representative = messages
            .mapIndexed { index, message -> index to classifier.classify(message) }
            .maxWithOrNull(compareBy({ it.second.confidence }, { it.first }))
            ?.second
        return representative?.let { ConversationEmotion(speaker, it) }
    }
}
