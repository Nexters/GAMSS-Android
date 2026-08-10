package com.gamss.android.data.remote.conversation

import com.gamss.android.data.remote.conversation.model.request.SaveMessageRequest
import com.gamss.android.data.remote.gamssJson
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SaveMessageRequestTest {

    @Test
    fun 새_채팅방_요청은_conversationId_와_답장_대상을_보내지_않는다() {
        val encoded = gamssJson.encodeToString(SaveMessageRequest(content = "오늘 억울한 일이 있었어"))

        val keys = gamssJson.parseToJsonElement(encoded).jsonObject.keys
        assertEquals(setOf("content"), keys)
    }

    @Test
    fun 이어_쓰기와_답장은_해당_필드를_함께_보낸다() {
        val encoded = gamssJson.encodeToString(
            SaveMessageRequest(content = "고마워", conversationId = 7L, repliesToMessageId = 42L),
        )

        val keys = gamssJson.parseToJsonElement(encoded).jsonObject.keys
        assertEquals(setOf("content", "conversationId", "repliesToMessageId"), keys)
        assertFalse(encoded.contains("null"))
    }
}
