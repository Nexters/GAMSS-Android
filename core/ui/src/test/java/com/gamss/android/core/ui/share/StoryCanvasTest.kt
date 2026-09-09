package com.gamss.android.core.ui.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 캔버스 좌표는 실수 계산이라 픽셀 단위 이하 오차는 허용한다. */
private const val TOLERANCE = 0.01f

class StoryCanvasTest {

    @Test
    fun `카드를 캔버스 가운데에 놓는다`() {
        val bounds = requireNotNull(storyCardBounds(cardWidth = 366, cardHeight = 500))

        assertEquals(STORY_WIDTH / 2f, (bounds.left + bounds.right) / 2f, TOLERANCE)
        assertEquals(STORY_HEIGHT / 2f, (bounds.top + bounds.bottom) / 2f, TOLERANCE)
    }

    @Test
    fun `원본 비율을 유지한다`() {
        val bounds = requireNotNull(storyCardBounds(cardWidth = 366, cardHeight = 500))

        assertEquals(366f / 500f, bounds.width / bounds.height, TOLERANCE)
    }

    @Test
    fun `세로로 긴 카드는 높이 여백에 먼저 닿는다`() {
        val bounds = requireNotNull(storyCardBounds(cardWidth = 100, cardHeight = 1000))

        // 상하 여백 10% 를 뺀 높이에 딱 맞고, 좌우로는 여백보다 더 남는다.
        assertEquals(STORY_HEIGHT * 0.8f, bounds.height, TOLERANCE)
        assertTrue(bounds.left > STORY_WIDTH * 0.16f)
    }

    @Test
    fun `가로로 넓은 카드는 폭 여백에 먼저 닿는다`() {
        val bounds = requireNotNull(storyCardBounds(cardWidth = 1000, cardHeight = 100))

        // 좌우 여백 16% 씩을 뺀 폭에 딱 맞고, 위아래로는 여백보다 더 남는다.
        assertEquals(STORY_WIDTH * 0.68f, bounds.width, TOLERANCE)
        assertTrue(bounds.top > STORY_HEIGHT * 0.10f)
    }

    @Test
    fun `여백 안에 들어간다`() {
        val bounds = requireNotNull(storyCardBounds(cardWidth = 366, cardHeight = 500))

        assertTrue(bounds.left >= STORY_WIDTH * 0.16f - TOLERANCE)
        assertTrue(bounds.right <= STORY_WIDTH * 0.84f + TOLERANCE)
        assertTrue(bounds.top >= STORY_HEIGHT * 0.10f - TOLERANCE)
        assertTrue(bounds.bottom <= STORY_HEIGHT * 0.90f + TOLERANCE)
    }

    @Test
    fun `크기가 0 인 캡처는 놓을 자리가 없다`() {
        assertNull(storyCardBounds(cardWidth = 0, cardHeight = 500))
        assertNull(storyCardBounds(cardWidth = 366, cardHeight = 0))
    }
}
