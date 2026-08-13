package com.gamss.android.data.remote.conversation

import com.gamss.android.data.remote.conversation.model.response.ConversationResponse
import com.gamss.android.data.remote.conversation.model.response.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationResponseTest {

    @Test
    fun 아직_제목이_없는_방은_null_그대로_전달한다() {
        val conversation = ConversationResponse(id = 37, title = null).toDomain()

        assertEquals(37L, conversation?.id)
        assertNull(conversation?.title)
    }

    @Test
    fun 방_id_가_없는_항목은_버린다() {
        assertNull(ConversationResponse(id = null, title = "팀장이 아이디어 가로챘어").toDomain())
    }
}
