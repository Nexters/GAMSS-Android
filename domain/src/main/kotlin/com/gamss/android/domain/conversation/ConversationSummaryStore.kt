package com.gamss.android.domain.conversation

import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.UtteranceTokenCounter
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * 압축본 = 첫 발화 원문 + 확정 청크들 + 최근 [RECENT_RAW_UTTERANCES] 발화 원문.
 * 첫 발화는 주제를, 최근 발화는 답글이 이어받는 맥락을 붙들고 있어 원문으로 남긴다.
 * 그 사이만 요약하고, 한 번 확정한 청크는 다시 요약하지 않는다(요약의 요약을 피한다).
 *
 * 압축은 부가 기능이라 실패해도 전송·조회를 막지 않는다.
 *
 * 가변 상태를 들고 있어 대화 하나에 인스턴스 하나여야 한다. 스코프를 붙이면 대화 간에 내용이 섞인다.
 */
class ConversationSummaryStore @Inject constructor(
    private val summarizer: DiarySummarizer,
    private val tokenCounter: UtteranceTokenCounter,
) {
    /** orbit intent 는 서로 다른 스레드에서 동시에 재개될 수 있다. */
    private val mutex = Mutex()

    private val utterances = mutableListOf<String>()

    /** 요약본이거나, 요약이 실패했으면 원문이다. */
    private val closedChunks = mutableListOf<String>()

    private val pendingChunk = mutableListOf<String>()
    private var pendingChunkTokens = 0

    /** 첫 발화는 원문으로 남으므로 청크 대상에서 제외한다. */
    private var nextChunkCandidate = 1

    suspend fun add(utterance: String) {
        val trimmed = utterance.trim()
        if (trimmed.isEmpty()) return
        mutex.withLock {
            utterances += trimmed
            closeChunksIfNeeded()
        }
    }

    suspend fun addAll(newUtterances: List<String>) {
        newUtterances.forEach { add(it) }
    }

    /** 보낼 압축본. 아직 압축할 게 없으면 null. */
    suspend fun current(): String? = mutex.withLock {
        if (utterances.isEmpty()) return@withLock null

        val recentStart = recentStart()
        // 첫 발화와 최근 발화는 상한 안에서 항상 살린다.
        val anchorHead = if (recentStart > 0) listOf(utterances.first()) else emptyList()
        val anchorTail = utterances.subList(recentStart, utterances.size).toList()
        val middle = closedChunks + pendingChunk

        val anchorLength = joinedLength(anchorHead + anchorTail)
        val kept = middle.takeNewestFitting(MAX_CONTEXT_SUMMARY_LENGTH - anchorLength)

        (anchorHead + kept + anchorTail)
            .joinToString(separator = SEPARATOR)
            .take(MAX_CONTEXT_SUMMARY_LENGTH)
    }

    suspend fun reset() = mutex.withLock {
        utterances.clear()
        closedChunks.clear()
        pendingChunk.clear()
        pendingChunkTokens = 0
        nextChunkCandidate = 1
    }

    private fun recentStart(): Int = (utterances.size - RECENT_RAW_UTTERANCES).coerceAtLeast(0)

    private suspend fun closeChunksIfNeeded() {
        val recentStart = recentStart()
        while (nextChunkCandidate < recentStart) {
            val utterance = utterances[nextChunkCandidate]
            val tokens = countTokens(utterance)
            // 예산을 넘겨서 요약하면 요약기 입력 한계에서 초과분이 잘려 사라진다.
            if (pendingChunk.isNotEmpty() && pendingChunkTokens + tokens > SUMMARY_CHUNK_TOKEN_BUDGET) {
                closePendingChunk()
            }
            pendingChunk += utterance
            pendingChunkTokens += tokens
            nextChunkCandidate++
            if (pendingChunkTokens >= SUMMARY_CHUNK_TOKEN_BUDGET) {
                closePendingChunk()
            }
        }
    }

    /** 요약 실패 시 원문으로 확정한다. 열어 두면 전송마다 같은 요약을 재시도해 지연이 누적된다. */
    private suspend fun closePendingChunk() {
        if (pendingChunk.isEmpty()) return
        val text = pendingChunk.joinToString(separator = SEPARATOR)
        // 발화 하나가 예산보다 크면 요약해도 잘리므로 원문으로 둔다.
        val overBudget = pendingChunkTokens > SUMMARY_CHUNK_TOKEN_BUDGET
        closedChunks += if (overBudget) text else summarizeOrNull(text) ?: text
        pendingChunk.clear()
        pendingChunkTokens = 0
    }

    private suspend fun summarizeOrNull(text: String): String? = failSafe { summarizer.summarize(text) }

    /** 토크나이저가 죽으면 글자 수로 센다. 실제 토큰 수의 상한이라 예산을 넘기지 않는다. */
    private suspend fun countTokens(utterance: String): Int =
        failSafe { tokenCounter.count(utterance) } ?: utterance.length

    /** 취소는 삼키지 않는다. */
    private suspend fun <T> failSafe(block: suspend () -> T): T? {
        val result = runCatching { block() }
        currentCoroutineContext().ensureActive()
        return result.getOrNull()
    }

    /** 예산에 맞을 때까지 오래된 항목부터 버리고 원래 순서로 돌려준다. */
    private fun List<String>.takeNewestFitting(budget: Int): List<String> {
        if (budget <= 0) return emptyList()
        var remaining = budget
        val kept = ArrayDeque<String>()
        for (part in asReversed()) {
            val cost = part.length + SEPARATOR.length
            if (cost > remaining) break
            remaining -= cost
            kept.addFirst(part)
        }
        return kept
    }

    private fun joinedLength(parts: List<String>): Int =
        parts.sumOf { it.length + SEPARATOR.length }

    private companion object {
        const val SEPARATOR = " "
    }
}
