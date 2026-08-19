package com.gamss.android.domain.conversation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

const val PENDING_REVEAL_EXPIRY_MILLIS = 30_000L

@Singleton
class PendingConversationReveal @Inject constructor() {

    private val mutex = Mutex()

    private var pending: Entry? = null

    suspend fun save(sent: SentMessage, now: Long = System.currentTimeMillis()) {
        mutex.withLock { pending = Entry(sent, now) }
    }

    suspend fun consume(conversationId: Long, now: Long = System.currentTimeMillis()): PendingReveal? =
        mutex.withLock {
            val current = pending?.takeIf { it.sent.message.conversationId == conversationId } ?: return@withLock null
            pending = null
            current.takeIf { now - it.savedAtMillis <= PENDING_REVEAL_EXPIRY_MILLIS }?.toPendingReveal()
        }

    private data class Entry(val sent: SentMessage, val savedAtMillis: Long) {
        fun toPendingReveal() = PendingReveal(
            sent = sent,
            createdAt = Instant.ofEpochMilli(savedAtMillis).atZone(ZoneId.systemDefault()).toLocalDateTime(),
        )
    }
}

/**
 * [PendingConversationReveal.consume] 결과. 서버 대화 상세를 다시 받아오기 전이라 [createdAt] 은
 * 서버 값이 아니라 [PendingConversationReveal.save] 가 호출된(=대화가 막 생긴) 시각이다.
 */
data class PendingReveal(
    val sent: SentMessage,
    val createdAt: LocalDateTime,
)
