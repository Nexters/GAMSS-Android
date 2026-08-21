package com.gamss.android.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.archive.navigation.ArchiveDetailKey
import com.gamss.android.feature.archive.navigation.ArchiveKey
import com.gamss.android.feature.carddelete.navigation.CardDeleteKey
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.home.navigation.HomeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {

    private fun navigator() = Navigator(
        NavigationState(
            startKey = HomeKey,
            topLevelStack = NavBackStack<NavKey>(HomeKey),
            subStacks = mapOf(
                ArchiveKey to NavBackStack<NavKey>(ArchiveKey),
                HomeKey to NavBackStack<NavKey>(HomeKey),
                ChatKey to NavBackStack<NavKey>(ChatKey),
            ),
        ),
    )

    private fun Navigator.openArchiveDetail() {
        navigate(ArchiveKey)
        navigate(ArchiveDetailKey(EmotionCharacter.ANGER))
    }

    @Test
    fun 보관함_상세에서_비우기를_확인하면_전체_파쇄_화면이_열린다() {
        val navigator = navigator()
        navigator.openArchiveDetail()

        navigator.navigate(CardDeleteKey())

        assertEquals(CardDeleteKey(), navigator.state.currentKey)
        assertEquals(
            listOf(ArchiveKey, ArchiveDetailKey(EmotionCharacter.ANGER), CardDeleteKey()),
            navigator.state.currentSubStack.toList(),
        )
    }

    @Test
    fun 카드_상세에서_버리면_그_카드의_파쇄_화면이_열린다() {
        val navigator = navigator()
        navigator.openArchiveDetail()

        navigator.navigate(CardDeleteKey(CARD_ID))

        assertEquals(CardDeleteKey(CARD_ID), navigator.state.currentKey)
    }

    /** 전체를 비우면 돌아갈 칸이 비어 있으므로, 상세까지 걷어내고 보관함 첫 화면만 남긴다. */
    @Test
    fun 전체_파쇄를_마치면_상세까지_걷어내고_보관함_첫_화면으로_돌아온다() {
        val navigator = navigator()
        navigator.openArchiveDetail()
        navigator.navigate(CardDeleteKey())

        navigator.finishCurrentFlow()

        assertEquals(ArchiveKey, navigator.state.currentKey)
        assertEquals(listOf(ArchiveKey), navigator.state.currentSubStack.toList())
    }

    /** 한 장만 지웠으면 남은 종이를 봐야 하므로 보던 칸으로 돌아간다. */
    @Test
    fun 카드_한_장_파쇄를_마치면_보관함_상세로_돌아가_목록을_다시_받는다() {
        val navigator = navigator()
        navigator.openArchiveDetail()
        navigator.navigate(CardDeleteKey(CARD_ID))

        navigator.finishShreddedCard()

        assertEquals(ArchiveDetailKey(EmotionCharacter.ANGER), navigator.state.currentKey)
        assertTrue(navigator.consumeShreddedCard())
    }

    /** 신호를 비우지 않으면 그 칸에 다시 들어올 때마다 목록을 또 받는다. */
    @Test
    fun 파쇄_신호는_한_번만_읽힌다() {
        val navigator = navigator()
        navigator.openArchiveDetail()
        navigator.navigate(CardDeleteKey(CARD_ID))
        navigator.finishShreddedCard()

        navigator.consumeShreddedCard()

        assertFalse(navigator.consumeShreddedCard())
    }

    private companion object {
        const val CARD_ID = 7L
    }
}
