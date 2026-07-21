package com.gamss.android.domain.emotion

import javax.inject.Inject

/** 대화에서 뽑은 메인 화자와 그 화자의 대표 감정. */
data class ConversationEmotion(
    val mainSpeaker: String,
    val emotion: ClassificationResult,
)

/**
 * 카톡형 대화 텍스트 → 메인 화자 식별 → 그 화자의 발화를 모아 대표 감정을 분류한다.
 * 짧은 단문은 신호가 약하므로 메인 화자의 발화를 합쳐 한 번에 분류한다.
 */
class AnalyzeConversationEmotionUseCase @Inject constructor(
    private val classifier: EmotionClassifier,
) {
    suspend operator fun invoke(rawConversation: String): ConversationEmotion? {
        val turns = ConversationParser.parse(rawConversation)
        val speaker = turns.mainSpeaker() ?: return null
        val text = turns.asSequence()
            .filter { it.speaker == speaker }
            .joinToString(separator = " ") { it.message }
            .trim()
        return if (text.isEmpty()) null else ConversationEmotion(speaker, classifier.classify(text))
    }
}
