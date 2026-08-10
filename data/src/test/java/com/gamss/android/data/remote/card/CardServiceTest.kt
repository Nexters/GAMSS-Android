package com.gamss.android.data.remote.card

import com.gamss.android.data.remote.model.response.ApiResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class CardServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: CardService

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(CardService::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `cardId를 path로 보내고 카드 응답을 파싱한다`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(
                    """
                    {
                      "success": true,
                      "data": {
                        "id": 7,
                        "conversationId": 1,
                        "emotion": "ANGER",
                        "emotionLabel": "분노",
                        "summary": "오늘 비가 와서 짜증나고 찝찝하다",
                        "message": "얘 오늘 건들면 안 됨.",
                        "date": "2026-07-23"
                      },
                      "error": null
                    }
                    """.trimIndent(),
                )
                .build(),
        )

        val response = service.getCard(cardId = 7)

        assertEquals("GET /api/cards/7 HTTP/1.1", server.takeRequest().requestLine)
        assertEquals(true, response.success)
        assertEquals(7L, response.data?.id)
        assertEquals(1L, response.data?.conversationId)
        assertEquals("ANGER", response.data?.emotion)
        assertEquals("분노", response.data?.emotionLabel)
    }

    @Test
    fun `실패 envelope의 error code와 message를 파싱한다`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body(
                    """
                    {
                      "success": false,
                      "data": null,
                      "error": {
                        "code": "CARD_NOT_FOUND",
                        "message": "존재하지 않거나 이미 사라진 카드"
                      }
                    }
                    """.trimIndent(),
                )
                .build(),
        )

        val response: ApiResponse<*> = service.getCard(cardId = 404)

        assertEquals("GET /api/cards/404 HTTP/1.1", server.takeRequest().requestLine)
        assertEquals(false, response.success)
        assertEquals("CARD_NOT_FOUND", response.error?.code)
        assertEquals("존재하지 않거나 이미 사라진 카드", response.error?.message)
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
