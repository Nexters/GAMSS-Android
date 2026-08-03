package com.gamss.android.domain.conversation

import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.UtteranceTokenCounter
import javax.inject.Inject

/**
 * 서버 생성 컨텍스트로 보낼 압축본을 대화가 진행되는 동안 만들어 둔다.
 *
 * 압축본 = 첫 발화 원문 + 확정 청크 요약들 + 최근 [RECENT_RAW_UTTERANCES] 발화 원문.
 * 첫 발화는 대화의 주제를 붙들고 있어 남기고, 최근 발화는 답글이 직접 이어받는 맥락이라 원문으로 둔다.
 * 그 사이만 요약하되 청크를 한 번 확정하면 다시 요약하지 않는다(요약의 요약을 피한다).
 *
 * 가변 상태를 들고 있어 대화 하나에 인스턴스 하나여야 한다. 스코프 애노테이션을 붙이면 대화 간에
 * 내용이 섞이므로 무스코프로 두어야 한다. 스레드 안전하지 않다.
 */
class ConversationSummaryStore @Inject constructor(
    private val summarizer: DiarySummarizer,
    private val tokenCounter: UtteranceTokenCounter,
) {
    private val utterances = mutableListOf<String>()

    /** 확정된 청크 요약. 같은 텍스트를 두 번 요약하지 않기 위한 캐시다. */
    private val chunkSummaries = mutableListOf<String>()

    /** 요약을 기다리는 발화. 토큰 예산을 채우면 요약해 [chunkSummaries] 로 옮긴다. */
    private val pendingChunk = mutableListOf<String>()
    private var pendingChunkTokens = 0

    /** 최근 발화 창에서 밀려난 발화만 청크 대상이 된다. 첫 발화는 원문으로 남으므로 제외한다. */
    private var nextChunkCandidate = 1

    suspend fun add(utterance: String) {
        val trimmed = utterance.trim()
        if (trimmed.isEmpty()) return
        utterances += trimmed
        closeChunksIfNeeded()
    }

    suspend fun addAll(utterances: List<String>) {
        utterances.forEach { add(it) }
    }

    /** 보낼 압축본. 아직 압축할 게 없으면 null. */
    fun current(): String? {
        if (utterances.isEmpty()) return null

        val recentStart = (utterances.size - RECENT_RAW_UTTERANCES).coerceAtLeast(0)
        val parts = buildList {
            // 최근 창이 첫 발화까지 덮으면 따로 붙이지 않는다(짧은 대화에서 중복 방지).
            if (recentStart > 0) add(utterances.first())
            addAll(chunkSummaries)
            addAll(pendingChunk)
            addAll(utterances.subList(recentStart, utterances.size))
        }
        return parts.joinToString(separator = " ").fitToLimit()
    }

    fun reset() {
        utterances.clear()
        chunkSummaries.clear()
        pendingChunk.clear()
        pendingChunkTokens = 0
        nextChunkCandidate = 1
    }

    /**
     * 최근 창에서 밀려난 발화를 청크에 넣고, 예산을 채우면 한 번 요약해 확정한다.
     * 요약 실패는 압축본을 못 만드는 것보다 원문을 남기는 편이 나으므로 청크를 열어 둔다.
     */
    private suspend fun closeChunksIfNeeded() {
        val recentStart = (utterances.size - RECENT_RAW_UTTERANCES).coerceAtLeast(0)
        while (nextChunkCandidate < recentStart) {
            val utterance = utterances[nextChunkCandidate]
            pendingChunk += utterance
            pendingChunkTokens += tokenCounter.count(utterance)
            nextChunkCandidate++

            if (pendingChunkTokens >= SUMMARY_CHUNK_TOKEN_BUDGET) {
                summarizePendingChunk()
            }
        }
    }

    private suspend fun summarizePendingChunk() {
        val text = pendingChunk.joinToString(separator = " ")
        val summary = runCatching { summarizer.summarize(text) }.getOrNull() ?: return
        chunkSummaries += summary
        pendingChunk.clear()
        pendingChunkTokens = 0
    }

    /**
     * 상한을 넘기면 전송이 거절되므로 반드시 맞춘다. 오래된 청크 요약부터 버려 최근 맥락을 지키고,
     * 그래도 남으면 잘라낸다. 전송 경로에서 도는 코드라 여기서 다시 요약하지 않는다.
     */
    private fun String.fitToLimit(): String {
        if (length <= MAX_CONTEXT_SUMMARY_LENGTH) return this
        val fitted = (1..chunkSummaries.size)
            .asSequence()
            .map { rebuildWithoutOldestChunks(it) }
            .firstOrNull { it.length <= MAX_CONTEXT_SUMMARY_LENGTH }
        return fitted ?: take(MAX_CONTEXT_SUMMARY_LENGTH)
    }

    private fun rebuildWithoutOldestChunks(dropCount: Int): String {
        val recentStart = (utterances.size - RECENT_RAW_UTTERANCES).coerceAtLeast(0)
        val parts = buildList {
            if (recentStart > 0) add(utterances.first())
            addAll(chunkSummaries.drop(dropCount))
            addAll(pendingChunk)
            addAll(utterances.subList(recentStart, utterances.size))
        }
        return parts.joinToString(separator = " ")
    }
}
