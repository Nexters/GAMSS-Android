package com.gamss.android.app.navigation

import com.gamss.android.feature.calendar.navigation.CalendarKey
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.emotion.navigation.EmotionKey
import com.gamss.android.feature.home.navigation.HomeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun 목록은_원격_설정과_무관하게_항상_같다() {
        val release = topLevelDestinations(isDebug = false).keys()
        val debug = topLevelDestinations(isDebug = true).keys()

        assertEquals(setOf(CalendarKey, HomeKey, ChatKey), release)
        assertEquals(setOf(CalendarKey, HomeKey, ChatKey, EmotionKey), debug)
    }

    @Test
    fun 카드_기능이_꺼지면_보관함만_탭바에서_빠진다() {
        val visible = topLevelDestinations(isDebug = false).visibleIn(useCardFeature = false)

        assertEquals(listOf(HomeKey, ChatKey), visible.map { it.key })
    }

    @Test
    fun 카드_기능이_켜지면_보관함이_탭바에_들어간다() {
        val visible = topLevelDestinations(isDebug = false).visibleIn(useCardFeature = true)

        assertEquals(listOf(CalendarKey, HomeKey, ChatKey), visible.map { it.key })
    }

    @Test
    fun 디버그_탭은_카드_기능과_무관하게_따라온다() {
        val off = topLevelDestinations(isDebug = true).visibleIn(useCardFeature = false)
        val on = topLevelDestinations(isDebug = true).visibleIn(useCardFeature = true)

        assertEquals(listOf(HomeKey, ChatKey, EmotionKey), off.map { it.key })
        assertEquals(listOf(CalendarKey, HomeKey, ChatKey, EmotionKey), on.map { it.key })
    }

    /**
     * 네비게이션 key 집합이 원격 설정에 흔들리면 [rememberNavigationState] 의 백스택 저장 슬롯이 어긋난다.
     * 걸러내는 지점은 탭바뿐이어야 한다.
     */
    @Test
    fun 보관함을_걸러도_네비게이션_key_집합은_그대로다() {
        val destinations = topLevelDestinations(isDebug = false)

        assertTrue(CalendarKey in destinations.keys())
        assertFalse(CalendarKey in destinations.visibleIn(useCardFeature = false).map { it.key })
    }
}
