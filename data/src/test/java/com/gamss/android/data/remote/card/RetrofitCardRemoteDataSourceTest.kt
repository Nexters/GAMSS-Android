package com.gamss.android.data.remote.card

import com.gamss.android.data.remote.card.model.response.CardResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RetrofitCardRemoteDataSourceTest {

    @Test
    fun `service envelope의 data를 카드 응답으로 반환한다`() = runTest {
        val service = FakeCardService(
            response = ApiResponse(
                success = true,
                data = cardResponse(id = 7L),
            ),
        )
        val dataSource = RetrofitCardRemoteDataSource(service)

        val result = dataSource.getCard(cardId = 7L)

        assertEquals(7L, service.requestedCardId)
        assertEquals(7L, result.id)
        assertEquals("ANGER", result.emotion)
    }

    @Test
    fun `service envelope에 data가 없으면 예외를 던진다`() = runTest {
        val service = FakeCardService(
            response = ApiResponse(
                success = false,
                data = null,
            ),
        )
        val dataSource = RetrofitCardRemoteDataSource(service)

        val result = runCatching { dataSource.getCard(cardId = 7L) }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `service 예외는 그대로 전파한다`() = runTest {
        val failure = IOException("offline")
        val service = FakeCardService(failure = failure)
        val dataSource = RetrofitCardRemoteDataSource(service)

        val result = runCatching { dataSource.getCard(cardId = 7L) }

        assertSame(failure, result.exceptionOrNull())
    }

    private class FakeCardService(
        private val response: ApiResponse<CardResponse>? = null,
        private val failure: Throwable? = null,
    ) : CardService {
        var requestedCardId: Long? = null
            private set

        override suspend fun getCard(cardId: Long): ApiResponse<CardResponse> {
            requestedCardId = cardId
            failure?.let { throw it }
            return checkNotNull(response)
        }
    }

    private fun cardResponse(id: Long): CardResponse =
        CardResponse(
            id = id,
            conversationId = 1,
            emotion = "ANGER",
            emotionLabel = "분노",
            summary = "오늘 비가 와서 짜증나고 찝찝하다",
            message = "얘 오늘 건들면 안 됨.",
            date = "2026-07-23",
        )
}
