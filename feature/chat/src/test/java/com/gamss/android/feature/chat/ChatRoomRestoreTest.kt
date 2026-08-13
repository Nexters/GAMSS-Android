package com.gamss.android.feature.chat

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.orbitmvi.orbit.test.test

private const val SAVED_CONVERSATION_ID_KEY = "chatRoom.activeConversationId"

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRoomRestoreTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 홈에서_시작한_대화의_id_를_저장한다() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = chatRoomViewModel(savedStateHandle = savedStateHandle)

        viewModel.test(this) {
            containerHost.start(conversationId = null, initialMessage = INPUT)
            var state = awaitState()
            while (state.conversationId == null) {
                state = awaitState()
            }
            assertEquals(ROOM_ID, state.conversationId)
            cancelAndIgnoreRemainingItems()
        }

        assertEquals(ROOM_ID, savedStateHandle.get<Long>(SAVED_CONVERSATION_ID_KEY))
    }

    @Test
    fun 프로세스가_죽어도_저장된_대화를_이어받는다() = runTest {
        val repository = FakeConversationRepository()
        val savedStateHandle = SavedStateHandle(mapOf(SAVED_CONVERSATION_ID_KEY to ROOM_ID))
        val viewModel = chatRoomViewModel(
            conversationRepository = repository,
            savedStateHandle = savedStateHandle,
        )

        viewModel.test(this) {
            // NavKey 는 새 대화를 뜻하는 null 로 복원되고, 첫 문구는 이미 소비되어 넘어오지 않는다.
            containerHost.start(conversationId = null, initialMessage = null)
            var state = awaitState()
            while (state.conversationId == null) {
                state = awaitState()
            }
            assertEquals(ROOM_ID, state.conversationId)
            cancelAndIgnoreRemainingItems()
        }

        assertTrue(repository.sentContextSummaries.isEmpty())
    }
}
