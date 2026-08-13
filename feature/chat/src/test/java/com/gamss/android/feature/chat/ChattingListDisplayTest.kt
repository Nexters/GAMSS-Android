package com.gamss.android.feature.chat

import com.gamss.android.domain.conversation.Conversation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/** 날짜 묶음과 표시 문자열. 고정 시각만 쓰므로 기기 타임존과 로케일에 흔들리지 않는다. */
class ChattingListDisplayTest {

    @Test
    fun 같은_날짜는_한_묶음으로_모인다() {
        val groups = listOf(
            conversationAt(1, LocalDateTime.of(2026, 7, 31, 4, 20)),
            conversationAt(2, LocalDateTime.of(2026, 7, 31, 2, 43)),
        ).toConversationGroups()

        assertEquals(1, groups.size)
        assertEquals("26.07.31", groups.single().dateLabel)
        assertEquals(listOf(1L, 2L), groups.single().rows.map { it.id })
    }

    @Test
    fun 서버가_준_순서를_다시_정렬하지_않는다() {
        val groups = listOf(
            conversationAt(1, LocalDateTime.of(2026, 7, 29, 9, 0)),
            conversationAt(2, LocalDateTime.of(2026, 7, 31, 9, 0)),
            conversationAt(3, LocalDateTime.of(2026, 7, 30, 9, 0)),
        ).toConversationGroups()

        assertEquals(listOf("26.07.29", "26.07.31", "26.07.30"), groups.map { it.dateLabel })
    }

    @Test
    fun 시각을_모르는_방은_헤더_없는_마지막_묶음이_된다() {
        val groups = listOf(
            conversationAt(1, LocalDateTime.of(2026, 7, 31, 4, 20)),
            conversationAt(2, null),
        ).toConversationGroups()

        assertEquals(2, groups.size)
        assertEquals("26.07.31", groups.first().dateLabel)
        assertNull(groups.last().dateLabel)
        assertEquals(listOf(2L), groups.last().rows.map { it.id })
        assertNull(groups.last().rows.single().timeLabel)
    }

    @Test
    fun 모든_방이_시각을_모르면_묶음_하나만_남는다() {
        val groups = listOf(conversationAt(1, null), conversationAt(2, null)).toConversationGroups()

        assertEquals(1, groups.size)
        assertNull(groups.single().dateLabel)
        assertEquals(listOf(1L, 2L), groups.single().rows.map { it.id })
    }

    @Test
    fun 같은_id_가_두_번_오면_하나만_남긴다() {
        val groups = listOf(
            conversationAt(1, LocalDateTime.of(2026, 7, 31, 4, 20)),
            conversationAt(1, LocalDateTime.of(2026, 7, 31, 2, 43)),
        ).toConversationGroups()

        assertEquals(listOf(1L), groups.flatMap { group -> group.rows.map { it.id } })
    }

    @Test
    fun 빈_목록은_묶음도_없다() {
        assertEquals(emptyList<ConversationGroup>(), emptyList<Conversation>().toConversationGroups())
    }

    @Test
    fun 오전과_오후를_한국어로_표시한다() {
        assertEquals("오전 4:20", timeLabelOf(LocalDateTime.of(2026, 7, 31, 4, 20)))
        assertEquals("오후 11:05", timeLabelOf(LocalDateTime.of(2026, 7, 31, 23, 5)))
    }

    @Test
    fun 자정과_정오는_12시로_표시한다() {
        assertEquals("오전 12:00", timeLabelOf(LocalDateTime.of(2026, 7, 31, 0, 0)))
        assertEquals("오후 12:00", timeLabelOf(LocalDateTime.of(2026, 7, 31, 12, 0)))
    }

    @Test
    fun 제목이_없으면_null_그대로_전달한다() {
        val row = listOf(
            conversationAt(1, LocalDateTime.of(2026, 7, 31, 4, 20)).copy(title = null),
        ).toConversationGroups().single().rows.single()

        assertNull(row.title)
    }

    private fun conversationAt(id: Long, createdAt: LocalDateTime?): Conversation =
        Conversation(id = id, title = "대화 $id", createdAt = createdAt)

    private fun timeLabelOf(createdAt: LocalDateTime): String? =
        listOf(conversationAt(1, createdAt)).toConversationGroups().single().rows.single().timeLabel
}
