package com.gamss.android.data.repository

import com.gamss.android.core.common.AppResult
import com.gamss.android.core.common.network.ApiException
import com.gamss.android.data.remote.conversation.ConversationService
import com.gamss.android.data.remote.model.response.ApiError
import com.gamss.android.data.remote.model.response.ApiResponse
import com.gamss.android.domain.model.SessionExpiredException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ConversationRepositoryImplTest {

    private val conversationService: ConversationService = mockk()
    private val repository = ConversationRepositoryImpl(conversationService)

    @Test
    fun `삭제에 성공하면 성공으로 전한다`() = runTest {
        coEvery { conversationService.deleteConversation(ROOM_ID) } returns ApiResponse(success = true)

        val result = repository.deleteConversation(ROOM_ID)

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) { conversationService.deleteConversation(ROOM_ID) }
    }

    @Test
    fun `이미 삭제된 방은 성공으로 본다`() = runTest {
        coEvery { conversationService.deleteConversation(ROOM_ID) } throws
            httpException(CONFLICT, "CONVERSATION_ALREADY_DELETED")

        val result = repository.deleteConversation(ROOM_ID)

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun `없는 방은 실패로 전한다`() = runTest {
        coEvery { conversationService.deleteConversation(ROOM_ID) } throws
            httpException(NOT_FOUND, "CONVERSATION_NOT_FOUND")

        val result = repository.deleteConversation(ROOM_ID)

        assertEquals("CONVERSATION_NOT_FOUND", ((result as AppResult.Failure).throwable as ApiException).code)
    }

    @Test
    fun `남의 방은 실패로 전한다`() = runTest {
        coEvery { conversationService.deleteConversation(ROOM_ID) } throws
            httpException(FORBIDDEN, "CONVERSATION_ACCESS_DENIED")

        val result = repository.deleteConversation(ROOM_ID)

        assertEquals("CONVERSATION_ACCESS_DENIED", ((result as AppResult.Failure).throwable as ApiException).code)
    }

    /**
     * 에러 바디 파싱이 실패하면 code 가 null 이라 409 여도 흡수 대상인지 알 수 없다.
     * 모르는 채로 성공 처리하면 안 지워진 방을 지웠다고 보고하게 된다.
     */
    @Test
    fun `409여도 에러 코드를 읽지 못하면 흡수하지 않는다`() = runTest {
        val errorBody = "not json".toResponseBody("application/json".toMediaType())
        coEvery { conversationService.deleteConversation(ROOM_ID) } throws
            HttpException(Response.error<ApiResponse<Unit>>(CONFLICT, errorBody))

        val result = repository.deleteConversation(ROOM_ID)

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun `네트워크 오류는 흡수하지 않는다`() = runTest {
        coEvery { conversationService.deleteConversation(ROOM_ID) } throws IOException("offline")

        val result = repository.deleteConversation(ROOM_ID)

        assertTrue((result as AppResult.Failure).throwable is ApiException.Network)
    }

    @Test
    fun `인증 만료는 세션 만료 실패로 전한다`() = runTest {
        coEvery { conversationService.deleteConversation(ROOM_ID) } throws
            httpException(UNAUTHORIZED, "EXPIRED_TOKEN")

        val result = repository.deleteConversation(ROOM_ID)

        assertTrue((result as AppResult.Failure).throwable is SessionExpiredException)
    }

    /** 서버는 실패를 4xx/5xx 로 준다는 전제가 깨져도, 되돌릴 수 없는 삭제를 성공으로 보고하지 않는다. */
    @Test
    fun `200이어도 success가 false면 실패로 전한다`() = runTest {
        coEvery { conversationService.deleteConversation(ROOM_ID) } returns ApiResponse(
            success = false,
            error = ApiError(code = "CONVERSATION_NOT_FOUND", message = "없는 방"),
        )

        val result = repository.deleteConversation(ROOM_ID)

        assertEquals("CONVERSATION_NOT_FOUND", ((result as AppResult.Failure).throwable as ApiException).code)
    }

    /** 호출부의 `message ?: 기본문구` 폴백이 동작하도록 빈 문자열을 남기지 않는다. */
    @Test
    fun `200이고 success가 false인데 에러 정보가 없어도 메시지를 남긴다`() = runTest {
        coEvery { conversationService.deleteConversation(ROOM_ID) } returns ApiResponse(success = false)

        val result = repository.deleteConversation(ROOM_ID)

        val throwable = (result as AppResult.Failure).throwable
        assertTrue(throwable is ApiException)
        assertTrue(throwable.message.orEmpty().isNotBlank())
    }

    /**
     * 같은 인증 실패가 4xx 로 오면 세션 만료가 된다. envelope 로 왔다고 일반 실패로 흘리면
     * 유스케이스의 세션 만료 승격을 우회해 재로그인 신호가 부분 성공에 묻힌다.
     */
    @Test
    fun `200이어도 인증 만료 코드면 세션 만료로 전한다`() = runTest {
        coEvery { conversationService.deleteConversation(ROOM_ID) } returns ApiResponse(
            success = false,
            error = ApiError(code = "EXPIRED_TOKEN", message = "만료"),
        )

        val result = repository.deleteConversation(ROOM_ID)

        assertTrue((result as AppResult.Failure).throwable is SessionExpiredException)
    }

    @Test
    fun `200이어도 이미 삭제된 방 코드면 성공으로 본다`() = runTest {
        coEvery { conversationService.deleteConversation(ROOM_ID) } returns ApiResponse(
            success = false,
            error = ApiError(code = "CONVERSATION_ALREADY_DELETED", message = "이미 삭제됨"),
        )

        val result = repository.deleteConversation(ROOM_ID)

        assertTrue(result is AppResult.Success)
    }

    /**
     * 삭제가 이 흡수 패턴을 복제하는데 정작 원본인 종료 쪽에 테스트가 없었다.
     * 패턴을 먼저 고정해 둔다.
     */
    @Test
    fun `이미 종료된 방은 종료 성공으로 본다`() = runTest {
        coEvery { conversationService.endConversation(ROOM_ID) } throws
            httpException(CONFLICT, "CONVERSATION_ALREADY_ENDED")

        val result = repository.endConversation(ROOM_ID)

        assertTrue(result is AppResult.Success)
    }

    /** 종료도 응답 본문을 쓰지 않아 삭제와 같은 구멍이 있었다. 종료된 줄 알고 카드 생성으로 넘어가면 안 된다. */
    @Test
    fun `200이어도 success가 false면 종료 실패로 전한다`() = runTest {
        coEvery { conversationService.endConversation(ROOM_ID) } returns ApiResponse(
            success = false,
            error = ApiError(code = "CONVERSATION_NOT_FOUND", message = "없는 방"),
        )

        val result = repository.endConversation(ROOM_ID)

        assertEquals("CONVERSATION_NOT_FOUND", ((result as AppResult.Failure).throwable as ApiException).code)
    }

    private fun httpException(status: Int, code: String): HttpException {
        val errorBody = """{"success":false,"error":{"code":"$code","message":"failed"}}"""
            .toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<ApiResponse<Unit>>(status, errorBody))
    }

    private companion object {
        const val ROOM_ID = 7L
        const val UNAUTHORIZED = 401
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404
        const val CONFLICT = 409
    }
}
