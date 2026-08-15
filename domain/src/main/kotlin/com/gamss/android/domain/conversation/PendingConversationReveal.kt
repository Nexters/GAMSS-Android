package com.gamss.android.domain.conversation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

const val PENDING_REVEAL_EXPIRY_MILLIS = 30_000L

/**
 * 홈에서 새 대화를 열며 받은 첫 교환을, 뒤이어 열리는 채팅방이 한 번 소비하도록 건네주는 우편함이다.
 *
 * [ConversationSession]은 대화별 감정·요약 상태를 들고 있어 무스코프라(홈과 채팅방이 각자 다른
 * 인스턴스를 받는다), 그 안에 캐시를 두면 홈 쪽 인스턴스에만 남고 채팅방 쪽으로 건너가지 않는다.
 * 이 클래스는 최근 값 하나만 잠깐 들고 있다가 소비되면 비우는 우편함이라 인스턴스 간에 공유해도
 * 대화 상태가 섞일 일이 없어 [Singleton]으로 둔다.
 *
 * 홈→채팅방 이동이 중간에 끊기면(back 경쟁, 화면 destroy 등) 값이 소비되지 못한 채 남을 수 있다.
 * 그 상태로 한참 뒤 같은 대화를 다시 열면(예: 목록에서) 오래된 첫 교환만 보여주고 서버의 실제
 * 전체 이력([restore])을 건너뛰게 된다. 정상적인 홈→채팅방 전환은 화면 전환 수준으로 즉시
 * 끝나므로, [PENDING_REVEAL_EXPIRY_MILLIS] 를 넉넉히 두고 그보다 오래된 값은 폐기해 [restore] 로
 * 폴백시킨다.
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
