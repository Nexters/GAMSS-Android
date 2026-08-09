package com.gamss.android.data.repository

import com.gamss.android.data.remote.conversation.ConversationService
import com.gamss.android.data.remote.conversation.model.request.UpdateConversationTitleRequest
import com.gamss.android.data.remote.conversation.model.response.ConversationResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationRepositoryImplTest {

    private val conversationService: ConversationService = mockk()

    @Test
    fun `호출자가 취소돼도 제목 지정 요청은 끝까지 간다`() = runTest {
        var completed = false
        coEvery { conversationService.updateTitle(any(), any()) } coAnswers {
            delay(REQUEST_MILLIS)
            completed = true
            ApiResponse(success = true, data = ConversationResponse(id = ROOM_ID, title = TITLE))
        }
        val repository = ConversationRepositoryImpl(
            conversationService = conversationService,
            applicationScope = this,
        )

        val caller = launch { repository.updateTitle(conversationId = ROOM_ID, title = TITLE) }
        advanceTimeBy(REQUEST_MILLIS / 2)
        caller.cancel()
        advanceUntilIdle()

        assertTrue("취소 시점에 요청이 끊겼다", completed)
        coVerify(exactly = 1) {
            conversationService.updateTitle(ROOM_ID, UpdateConversationTitleRequest(TITLE))
        }
    }

    private companion object {
        const val ROOM_ID = 7L
        const val TITLE = "팀장이 아이디어 가로챘어"
        const val REQUEST_MILLIS = 100L
    }
}
