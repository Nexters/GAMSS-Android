package com.gamss.android.app.navigation

import com.gamss.android.feature.archive.navigation.ArchiveKey
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.home.navigation.HomeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun 탭은_보관함_홈_대화_세_종이다() {
        assertEquals(listOf(ArchiveKey, HomeKey, ChatKey), topLevelDestinations().map { it.key })
    }

    @Test
    fun 카드_기능이_꺼지면_보관함만_탭바에서_빠진다() {
        val visible = topLevelDestinations().visibleIn(useCardFeature = false)

        assertEquals(listOf(HomeKey, ChatKey), visible.map { it.key })
    }

    @Test
    fun 카드_기능이_켜지면_보관함이_탭바에_들어간다() {
        val visible = topLevelDestinations().visibleIn(useCardFeature = true)

        assertEquals(listOf(ArchiveKey, HomeKey, ChatKey), visible.map { it.key })
    }

    /**
     * 네비게이션 key 집합이 원격 설정에 흔들리면 [rememberNavigationState] 의 백스택 저장 슬롯이 어긋난다.
     * 걸러내는 지점은 탭바뿐이어야 한다.
     */
    @Test
    fun 보관함을_걸러도_네비게이션_key_집합은_그대로다() {
        val destinations = topLevelDestinations()

        assertTrue(ArchiveKey in destinations.keys())
        assertFalse(ArchiveKey in destinations.visibleIn(useCardFeature = false).map { it.key })
    }
}
