package com.gamss.android.domain.emotion

import com.gamss.android.domain.common.failSafe
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * 발화가 생길 때마다 분류해 점수를 누적한다. 종료 시점에 몰아서 분류하지 않으려는 용도이고,
 * 결과는 [ClassifyUserEmotionUseCase] 로 한꺼번에 계산한 것과 동일하다.
 *
 * 감정 누적은 카드용 부가 기능이라 분류가 실패해도 대화를 막지 않는다. 실패한 발화는 [MAX_ATTEMPTS] 번까지
 * 다시 시도하고, 그때까지도 안 되면 건너뛴다. 무한히 붙들면 뒤 발화가 영영 누적되지 않는다.
 *
 * 가변 상태를 들고 있어 대화 하나에 인스턴스 하나여야 한다. 스코프 애노테이션을 붙이면
 * (예: `@Singleton`) 대화 간에 점수가 섞이므로 무스코프로 두어야 한다.
 */
class ConversationEmotionAccumulator @Inject constructor(
    private val classifier: EmotionClassifier,
) {
    private val stateMutex = Mutex()

    private val classificationMutex = Mutex()

    private val pending = ArrayDeque<String>()
    private val summed = LinkedHashMap<String, Float>()
    private var utteranceCount = 0
    private var attemptsAtHead = 0

    /** 발화만 적재한다. 분류는 [classifyPending] 에서 돈다. */
    suspend fun append(utterance: String) {
        val trimmed = utterance.trim()
        if (trimmed.isEmpty()) return
        stateMutex.withLock { pending += trimmed }
    }

    suspend fun restore(history: List<String>) {
        val trimmed = history.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
        stateMutex.withLock {
            clearLocked()
            pending += trimmed
        }
    }

    suspend fun reset() = stateMutex.withLock { clearLocked() }

    /** @return 큐를 다 비웠으면 true. 분류에 실패해 다시 시도할 발화를 남겼으면 false. */
    suspend fun classifyPending(): Boolean = classificationMutex.withLock {
        var head = nextPending()
        while (head != null && classifyHead(head)) {
            head = nextPending()
        }
        head == null
    }

    suspend fun result(): EmotionResult? = stateMutex.withLock {
        aggregateEmotion(summed, utteranceCount)
    }

    private suspend fun nextPending(): String? = stateMutex.withLock { pending.firstOrNull() }

    /** @return 다음 발화로 넘어가도 되면 true. 이 자리를 남기고 멈춰야 하면 false. */
    private suspend fun classifyHead(utterance: String): Boolean {
        val scores = failSafe { classifier.classify(utterance).scores }
        return stateMutex.withLock {
            when {
                scores != null -> {
                    scores.forEach { (label, score) -> summed[label] = (summed[label] ?: 0f) + score }
                    utteranceCount++
                    dropHeadLocked()
                    true
                }
                // 같은 자리에서 계속 실패하면 뒤 발화까지 막히므로 포기하고 넘어간다.
                ++attemptsAtHead >= MAX_ATTEMPTS -> {
                    dropHeadLocked()
                    true
                }
                else -> false
            }
        }
    }

    private fun dropHeadLocked() {
        pending.removeFirstOrNull()
        attemptsAtHead = 0
    }

    private fun clearLocked() {
        pending.clear()
        summed.clear()
        utteranceCount = 0
        attemptsAtHead = 0
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
    }
}
