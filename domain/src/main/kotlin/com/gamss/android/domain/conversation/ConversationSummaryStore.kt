package com.gamss.android.domain.conversation

import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.UtteranceTokenCounter
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * 첫 발화는 주제를, 최근 [RECENT_RAW_UTTERANCES] 발화는 답글이 이어받는 맥락을 붙들고 있어 원문으로 남긴다.
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
    /** 상태 변이용. 요약은 이 락 밖에서 돌려 전송 경로가 요약을 기다리지 않게 한다. */
    private val stateMutex = Mutex()

    /** 요약을 하나씩 돌려 [closedChunks] 의 순서를 보장한다. */
    private val compactionMutex = Mutex()

    private val utterances = mutableListOf<String>()

    /** 요약본이거나, 요약을 건너뛴 구간의 원문이다. */
    private val closedChunks = mutableListOf<String>()

    private val pendingChunk = mutableListOf<String>()
    private var pendingChunkTokens = 0

    /** 첫 발화는 원문으로 남으므로 청크 대상에서 제외한다. */
    private var nextChunkCandidate = FIRST_CHUNK_CANDIDATE

    suspend fun add(utterance: String) {
        val trimmed = utterance.trim()
        if (trimmed.isEmpty()) return
        stateMutex.withLock { utterances += trimmed }
        compact()
    }

    /**
     * 재진입 복원. 히스토리를 요약하지 않는다. 진입만으로 온디바이스 요약이 발화 수에 비례해 터진다.
     * 중간 발화는 원문 청크로 두고 상한은 [currentContextSummary] 가 최근 쪽부터 맞춘다.
     */
    suspend fun restore(history: List<String>) {
        val trimmed = history.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
        stateMutex.withLock {
            clearLocked()
            if (trimmed.isEmpty()) return@withLock
            utterances += trimmed
            val recentStart = recentStartLocked()
            closedChunks += trimmed.subList(FIRST_CHUNK_CANDIDATE.coerceAtMost(recentStart), recentStart)
            nextChunkCandidate = recentStart
        }
    }

    suspend fun currentContextSummary(): String? = stateMutex.withLock {
        if (utterances.isEmpty()) return@withLock null

        val recentStart = recentStartLocked()
        val anchorHead = if (recentStart > 0) listOf(utterances.first()) else emptyList()
        val anchorTail = utterances.subList(recentStart, utterances.size).toList()
        val middle = closedChunks + pendingChunk

        val kept = middle.takeNewestFitting(MAX_CONTEXT_SUMMARY_LENGTH - budgetCost(anchorHead + anchorTail))

        (anchorHead + kept + anchorTail)
            .joinToString(separator = SEPARATOR)
            .take(MAX_CONTEXT_SUMMARY_LENGTH)
    }

    suspend fun reset() = stateMutex.withLock { clearLocked() }

    /**
     * 최근 창에서 밀려난 발화를 청크에 모아 예산이 차면 요약한다. 요약과 토큰 계산은 락 밖에서 돈다.
     */
    private suspend fun compact() = compactionMutex.withLock {
        var hasCandidate = true
        while (hasCandidate) {
            hasCandidate = compactNextCandidate()
        }
    }

    /** 후보가 남아 있으면 하나 처리하고 true. */
    private suspend fun compactNextCandidate(): Boolean {
        val candidate = stateMutex.withLock { nextCandidateLocked() } ?: return false
        val tokens = countTokens(candidate)
        val due = stateMutex.withLock { appendToPendingChunkLocked(candidate, tokens) }
        if (due != null) {
            val closed = if (due.keepRaw) due.text else summarizeOrNull(due.text) ?: due.text
            stateMutex.withLock { closedChunks += closed }
        }
        return true
    }

    private fun nextCandidateLocked(): String? =
        utterances.getOrNull(nextChunkCandidate)?.takeIf { nextChunkCandidate < recentStartLocked() }

    /** 확정할 청크. [keepRaw] 면 요약기 입력 한계를 넘겨 요약해도 잘리므로 원문으로 둔다. */
    private class DueChunk(val text: String, val keepRaw: Boolean)

    /** 청크가 예산에 닿으면 떼어 돌려준다. 아직이면 null. */
    private fun appendToPendingChunkLocked(candidate: String, tokens: Int): DueChunk? {
        // 예산을 넘겨서 요약하면 요약기 입력 한계에서 초과분이 잘려 사라진다.
        val closeBefore = pendingChunk.isNotEmpty() && pendingChunkTokens + tokens > SUMMARY_CHUNK_TOKEN_BUDGET
        val detached = if (closeBefore) detachPendingChunkLocked() else null

        pendingChunk += candidate
        pendingChunkTokens += tokens
        nextChunkCandidate++

        return detached ?: if (pendingChunkTokens >= SUMMARY_CHUNK_TOKEN_BUDGET) detachPendingChunkLocked() else null
    }

    private fun detachPendingChunkLocked(): DueChunk? {
        if (pendingChunk.isEmpty()) return null
        val due = DueChunk(
            text = pendingChunk.joinToString(separator = SEPARATOR),
            keepRaw = pendingChunkTokens > SUMMARY_CHUNK_TOKEN_BUDGET,
        )
        pendingChunk.clear()
        pendingChunkTokens = 0
        return due
    }

    private suspend fun summarizeOrNull(chunk: String): String? = failSafe { summarizer.summarize(chunk) }

    /** 토크나이저가 죽으면 글자 수로 센다. 실제 토큰 수의 상한이라 예산을 넘기지 않는다. */
    private suspend fun countTokens(utterance: String): Int =
        failSafe { tokenCounter.count(utterance) } ?: utterance.length

    private suspend fun <T> failSafe(block: suspend () -> T): T? {
        val result = runCatching { block() }
        currentCoroutineContext().ensureActive()
        return result.getOrNull()
    }

    private fun clearLocked() {
        utterances.clear()
        closedChunks.clear()
        pendingChunk.clear()
        pendingChunkTokens = 0
        nextChunkCandidate = FIRST_CHUNK_CANDIDATE
    }

    private fun recentStartLocked(): Int = (utterances.size - RECENT_RAW_UTTERANCES).coerceAtLeast(0)

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

    /** 마지막 구분자까지 세어 예산을 보수적으로 잡는다. */
    private fun budgetCost(parts: List<String>): Int = parts.sumOf { it.length + SEPARATOR.length }

    private companion object {
        const val SEPARATOR = " "
        const val FIRST_CHUNK_CANDIDATE = 1
    }
}
