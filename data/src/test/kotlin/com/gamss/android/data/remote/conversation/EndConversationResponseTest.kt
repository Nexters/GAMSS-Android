package com.gamss.android.data.remote.conversation

import com.gamss.android.data.remote.gamssJson
import com.gamss.android.data.remote.model.response.ApiResponse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 종료 응답은 본문을 읽지 않는다. 서버가 무엇을 담아 보내든 종료가 실패로 뒤집히면 안 된다.
 */
class EndConversationResponseTest {

    @Test
    fun 본문에_필드가_있어도_읽지_않고_성공으로_받는다() {
        val decoded = gamssJson.decodeFromString<ApiResponse<Unit>>(
            """{"success":true,"data":{"id":1,"status":"ENDED"}}""",
        )

        assertTrue(decoded.success)
    }

    @Test
    fun data_가_null_이어도_성공으로_받는다() {
        val decoded = gamssJson.decodeFromString<ApiResponse<Unit>>("""{"success":true,"data":null}""")

        assertTrue(decoded.success)
    }

    @Test
    fun data_가_없어도_성공으로_받는다() {
        val decoded = gamssJson.decodeFromString<ApiResponse<Unit>>("""{"success":true}""")

        assertTrue(decoded.success)
    }
}
