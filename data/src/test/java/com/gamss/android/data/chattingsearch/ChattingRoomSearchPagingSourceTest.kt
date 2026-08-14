package com.gamss.android.data.chattingsearch

import androidx.paging.PagingSource
import com.gamss.android.data.remote.conversation.ConversationService
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * 실제 서버가 내려주는 응답 그대로의 JSON을 MockWebServer에 태워, PagingSource가
 * 페이지를 정상적으로 파싱·연결하고 무한 스크롤(추가 로딩)이 마지막 페이지에서
 * 올바르게 멈추는지 검증한다.
 */
class ChattingRoomSearchPagingSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: ConversationService

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = retrofit.create(ConversationService::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `첫 페이지를 새로고침하면 서버 응답을 도메인 모델로 매핑하고 다음 페이지 키를 채운다`() = runTest {
        server.enqueue(jsonResponse(FIRST_PAGE_JSON))
        val pagingSource = pagingSource()

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false),
        )

        val page = result as PagingSource.LoadResult.Page
        Assert.assertEquals(2, page.data.size)
        Assert.assertEquals(1001L, page.data[0].conversationId)
        Assert.assertEquals("출근길에 느낀 답답함", page.data[0].title)
        Assert.assertEquals("ACTIVE", page.data[0].status)
        Assert.assertEquals("2026-08-04T08:12:00Z", page.data[0].createdAt)
        Assert.assertNull(page.prevKey)
        Assert.assertEquals(1, page.nextKey)

        val request = server.takeRequest()
        Assert.assertEquals("감정", request.url.queryParameter("keyword"))
        Assert.assertEquals("0", request.url.queryParameter("page"))
        Assert.assertEquals("20", request.url.queryParameter("size"))
    }

    @Test
    fun `다음 페이지를 추가 로딩하면 요청한 페이지 번호로 서버를 호출하고 이전-다음 키를 갱신한다`() = runTest {
        server.enqueue(jsonResponse(SECOND_PAGE_JSON))
        val pagingSource = pagingSource()

        val result = pagingSource.load(
            PagingSource.LoadParams.Append(key = 1, loadSize = 20, placeholdersEnabled = false),
        )

        val page = result as PagingSource.LoadResult.Page
        Assert.assertEquals(2, page.data.size)
        Assert.assertEquals(1003L, page.data[0].conversationId)
        Assert.assertEquals(0, page.prevKey)
        Assert.assertEquals(2, page.nextKey)
        Assert.assertEquals("1", server.takeRequest().url.queryParameter("page"))
    }

    @Test
    fun `마지막 페이지를 추가 로딩하면 다음 페이지 키가 없어 무한 스크롤이 멈춘다`() = runTest {
        server.enqueue(jsonResponse(LAST_PAGE_JSON))
        val pagingSource = pagingSource()

        val result = pagingSource.load(
            PagingSource.LoadParams.Append(key = 2, loadSize = 20, placeholdersEnabled = false),
        )

        val page = result as PagingSource.LoadResult.Page
        Assert.assertEquals(1, page.data.size)
        Assert.assertEquals(1, page.prevKey)
        Assert.assertNull(page.nextKey)
    }

    @Test
    fun `서버가 오류를 반환하면 로딩 실패로 반환된다`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(500)
                .body("""{"success":false,"error":{"code":"SEARCH_FAILED","message":"검색에 실패했습니다"}}""")
                .addHeader("Content-Type", "application/json")
                .build(),
        )
        val pagingSource = pagingSource()

        val result = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 20, placeholdersEnabled = false),
        )

        Assert.assertTrue(result is PagingSource.LoadResult.Error)
    }

    private fun pagingSource() = ChattingRoomSearchPagingSource(
        conversationService = service,
        keyword = "감정",
    )

    private fun jsonResponse(body: String) = MockResponse.Builder()
        .code(200)
        .body(body)
        .addHeader("Content-Type", "application/json")
        .build()

    private companion object {
        // 실제 서버 응답 규격(ApiResponse<ChattingRoomSearchResponse>)과 동일한 더미 JSON.
        val FIRST_PAGE_JSON = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "conversationId": 1001,
                    "title": "출근길에 느낀 답답함",
                    "status": "ACTIVE",
                    "createdAt": "2026-08-04T08:12:00Z"
                  },
                  {
                    "conversationId": 1002,
                    "title": "점심시간 감정 기록",
                    "status": "ACTIVE",
                    "createdAt": "2026-08-04T12:31:00Z"
                  }
                ],
                "page": 0,
                "size": 2,
                "totalElements": 5,
                "totalPages": 3
              }
            }
        """.trimIndent()

        val SECOND_PAGE_JSON = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "conversationId": 1003,
                    "title": "회의 후 정리한 생각",
                    "status": "ACTIVE",
                    "createdAt": "2026-08-03T22:18:00Z"
                  },
                  {
                    "conversationId": 1004,
                    "title": "친구와 나눈 고민",
                    "status": "ACTIVE",
                    "createdAt": "2026-08-03T23:40:00Z"
                  }
                ],
                "page": 1,
                "size": 2,
                "totalElements": 5,
                "totalPages": 3
              }
            }
        """.trimIndent()

        val LAST_PAGE_JSON = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "conversationId": 1005,
                    "title": "잠들기 전 감정 점검",
                    "status": "ACTIVE",
                    "createdAt": "2026-08-02T07:55:00Z"
                  }
                ],
                "page": 2,
                "size": 2,
                "totalElements": 5,
                "totalPages": 3
              }
            }
        """.trimIndent()
    }
}
