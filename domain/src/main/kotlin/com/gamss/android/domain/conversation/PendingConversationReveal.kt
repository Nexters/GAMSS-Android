package com.gamss.android.domain.conversation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    suspend fun consume(conversationId: Long, now: Long = System.currentTimeMillis()): SentMessage? =
        mutex.withLock {
            val current = pending?.takeIf { it.sent.message.conversationId == conversationId } ?: return@withLock null
            pending = null
            current.sent.takeIf { now - current.savedAtMillis <= PENDING_REVEAL_EXPIRY_MILLIS }
        }

    private data class Entry(val sent: SentMessage, val savedAtMillis: Long)
}
