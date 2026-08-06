package com.gamss.android.data.debug

import com.gamss.android.data.remote.chattingRoomSearch.model.response.ChattingRoomResponse
import com.gamss.android.data.remote.chattingRoomSearch.model.response.ChattingRoomSearchResponse
import com.gamss.android.data.remote.model.response.ApiResponse
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * `ChatSearchDebugActivity`(adb 전용, 실제 내비게이션에는 아직 연결되지 않음)로 채팅방 검색
 * 화면을 열었을 때, 실제 서버 대신 더미 데이터로 응답해 Paging 3 무한 스크롤 동작(추가 페이지
 * 로딩, 로딩 인디케이터, 마지막 페이지에서 멈추는지)을 실기기/에뮬레이터에서 눈으로 확인할 수
 * 있게 한다. `ChattingRoomSearchService`가 실제로 호출하는 엔드포인트만 가로채므로 다른 API는
 * 영향을 받지 않는다.
 *
 * 검색 기능이 실제 내비게이션에 연결되어 더 이상 더미 응답이 필요 없어지면 이 인터셉터와
 * [com.gamss.android.data.di.NetworkModule]의 등록 지점을 함께 제거한다.
 */
internal fun provideChattingRoomSearchDummyInterceptor(): Interceptor = Interceptor { chain ->
    val request = chain.request()
    if (request.url.encodedPath != SEARCH_PATH) {
        return@Interceptor chain.proceed(request)
    }

    // 실제 네트워크 지연을 흉내 내 로딩/추가 로딩 인디케이터가 화면에 보이도록 한다.
    Thread.sleep(FAKE_NETWORK_DELAY_MS)

    val keyword = request.url.queryParameter("keyword").orEmpty()
    val page = request.url.queryParameter("page")?.toIntOrNull() ?: 0
    val size = (request.url.queryParameter("size")?.toIntOrNull() ?: DEFAULT_SIZE).coerceAtLeast(1)

    val matched = DUMMY_ROOMS.filter { keyword.isBlank() || it.title.contains(keyword) }
    val fromIndex = (page * size).coerceIn(0, matched.size)
    val toIndex = (fromIndex + size).coerceIn(fromIndex, matched.size)

    val responseBody = ApiResponse(
        success = true,
        data = ChattingRoomSearchResponse(
            content = matched.subList(fromIndex, toIndex),
            page = page,
            size = size,
            totalElements = matched.size.toLong(),
            totalPages = if (matched.isEmpty()) 0 else (matched.size + size - 1) / size,
        ),
    )

    Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(
            json.encodeToString(
                ApiResponse.serializer(ChattingRoomSearchResponse.serializer()),
                responseBody,
            ).toResponseBody("application/json".toMediaType()),
        )
        .build()
}

private const val SEARCH_PATH = "/api/conversations/search"
private const val DEFAULT_SIZE = 5
private const val FAKE_NETWORK_DELAY_MS = 600L

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

// 45건: 기본 페이지 크기(20)로 3페이지(20/20/5)에 걸쳐 나뉘어, 실기기에서 두 번의
// 추가 로딩(append)을 스크롤로 직접 확인할 수 있다.
private val DUMMY_TITLES = listOf(
    "출근길에 느낀 답답함",
    "점심시간 감정 기록",
    "회의 후 정리한 생각",
    "친구와 나눈 고민",
    "잠들기 전 감정 점검",
    "아침 루틴과 기분 변화",
    "미뤄둔 일을 끝낸 날",
    "가족 대화 이후의 마음",
    "퇴근 후 산책 기록",
    "감정이 가라앉은 순간",
    "오랜만에 편안했던 저녁",
    "작은 성취감",
    "불안했지만 지나간 일",
)

private const val DUMMY_ROOM_COUNT = 45

private val DUMMY_ROOMS: List<ChattingRoomResponse> = (1..DUMMY_ROOM_COUNT).map { index ->
    val day = ((index % 28) + 1).toString().padStart(2, '0')
    ChattingRoomResponse(
        conversationId = 1000L + index,
        title = "${DUMMY_TITLES[(index - 1) % DUMMY_TITLES.size]} #$index",
        status = "ACTIVE",
        createdAt = "2026-08-${day}T08:12:00Z",
    )
}
