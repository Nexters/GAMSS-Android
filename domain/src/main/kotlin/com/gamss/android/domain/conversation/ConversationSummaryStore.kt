package com.gamss.android.domain.conversation

import com.gamss.android.domain.common.failSafe
import com.gamss.android.domain.summary.DiarySummarizer
import com.gamss.android.domain.summary.UtteranceTokenCounter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class ConversationSummaryStore @Inject constructor(
    private val summarizer: DiarySummarizer,
    private val tokenCounter: UtteranceTokenCounter,
) {
    private val stateMutex = Mutex()

    private val compactionMutex = Mutex()

    private val utterances = mutableListOf<String>()

    private val closedChunks = mutableListOf<String>()

    private val pendingChunk = mutableListOf<String>()
    private var pendingChunkTokens = 0

    private var nextChunkCandidate = FIRST_CHUNK_CANDIDATE

    /** 발화만 적재한다. 전체 히스토리를 매번 재요약하지 않도록 압축/재사용은 [compact] 가 맡는다. */
    suspend fun append(utterance: String) {
        val trimmed = utterance.trim()
        if (trimmed.isEmpty()) return
        stateMutex.withLock { utterances += trimmed }
    }

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

    /** 모델 다운로드를 미리 걸어둔다. 실패해도 무시 — 실제 요약 시점에 정식 경로로 다시 시도된다. */
    suspend fun prefetch() = summarizer.prefetch()

    suspend fun compact() = compactionMutex.withLock {
        var hasCandidate = true
        while (hasCandidate) {
            hasCandidate = compactNextCandidate()
        }
    }

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

    private class DueChunk(val text: String, val keepRaw: Boolean)

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

    private suspend fun countTokens(utterance: String): Int =
        failSafe { tokenCounter.count(utterance) } ?: utterance.length

    private fun clearLocked() {
        utterances.clear()
        closedChunks.clear()
        pendingChunk.clear()
        pendingChunkTokens = 0
        nextChunkCandidate = FIRST_CHUNK_CANDIDATE
    }

    private fun recentStartLocked(): Int = (utterances.size - RECENT_RAW_UTTERANCES).coerceAtLeast(0)

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

    private fun budgetCost(parts: List<String>): Int = parts.sumOf { it.length + SEPARATOR.length }

    private companion object {
        const val SEPARATOR = " "
        const val FIRST_CHUNK_CANDIDATE = 1
    }
}
