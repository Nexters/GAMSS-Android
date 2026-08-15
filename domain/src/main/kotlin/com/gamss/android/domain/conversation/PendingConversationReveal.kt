package com.gamss.android.domain.conversation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

const val PENDING_REVEAL_EXPIRY_MILLIS = 30_000L

/**
 * 홈에서 새 대화를 열며 받은 첫 교환을, 뒤이어 열리는 채팅방이 한 번 소비하도록 건네주는 우편함이다.
 * [ConversationSession]은 무스코프라 홈과 채팅방이 서로 다른 인스턴스를 받으므로 [Singleton]으로 둔다.
 * 소비되지 못한 값은 [PENDING_REVEAL_EXPIRY_MILLIS] 뒤 만료시켜 [restore] 로 폴백하게 한다.
 */
@Singleton
class PendingConversationReveal @Inject constructor() {

    private val mutex = Mutex()

    private var pending: Entry? = null

    suspend fun save(sent: SentMessage, now: Long = System.currentTimeMillis()) {
        mutex.withLock { pending = Entry(sent, now) }
    }

    suspend fun consume(conversationId: Long, now: Long = System.currentTimeMillis()): SentMessage? =
        mutex.withLock {
            val current = pending?.takeIf { it.sent.message.conversationId == conversationId } ?: return@withLock null
            pending = null
            current.sent.takeIf { now - current.savedAtMillis <= PENDING_REVEAL_EXPIRY_MILLIS }
        }

    private data class Entry(val sent: SentMessage, val savedAtMillis: Long)
}
