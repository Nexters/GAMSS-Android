package com.gamss.android.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.archive.navigation.ArchiveDetailKey
import com.gamss.android.feature.archive.navigation.ArchiveKey
import com.gamss.android.feature.carddelete.navigation.CardDeleteKey
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.chat.navigation.ChatRoomKey
import com.gamss.android.feature.home.navigation.HomeKey
import org.junit.Assert.assertEquals
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
        navigate(ArchiveDetailKey(EMOTION))
    }

    @Test
    fun 보관함_상세에서_비우기를_확인하면_그_칸의_파쇄_화면이_열린다() {
        val navigator = navigator()
        navigator.openArchiveDetail()

        navigator.navigate(CardDeleteKey(EMOTION))

        assertEquals(CardDeleteKey(EMOTION), navigator.state.currentKey)
        assertEquals(
            listOf(ArchiveKey, ArchiveDetailKey(EMOTION), CardDeleteKey(EMOTION)),
            navigator.state.currentSubStack.toList(),
        )
    }

    @Test
    fun 카드_상세에서_버리면_그_카드의_파쇄_화면이_열린다() {
        val navigator = navigator()
        navigator.openArchiveDetail()

        navigator.navigate(CardDeleteKey(EMOTION, CARD_ID))

        assertEquals(CardDeleteKey(EMOTION, CARD_ID), navigator.state.currentKey)
    }

    /** 칸을 비우면 돌아갈 자리가 비어 있으므로, 상세까지 걷어내고 보관함 첫 화면만 남긴다. */
    @Test
    fun 칸_파쇄를_마치면_상세까지_걷어내고_보관함_첫_화면으로_돌아온다() {
        val navigator = navigator()
        navigator.openArchiveDetail()
        navigator.navigate(CardDeleteKey(EMOTION))

        navigator.finishCurrentFlow()

        assertEquals(ArchiveKey, navigator.state.currentKey)
        assertEquals(listOf(ArchiveKey), navigator.state.currentSubStack.toList())
    }

    /** 한 장만 지웠으면 남은 종이를 봐야 하므로 보던 칸으로 돌아간다. */
    @Test
    fun 카드_한_장_파쇄를_마치면_보관함_상세로_돌아가_지운_카드를_알린다() {
        val navigator = navigator()
        navigator.openArchiveDetail()
        navigator.navigate(CardDeleteKey(EMOTION, CARD_ID))

        navigator.finishShreddedCard(CARD_ID)

        assertEquals(ArchiveDetailKey(EMOTION), navigator.state.currentKey)
        // 어느 카드를 뺄지 알려야 목록을 통째로 다시 받지 않고 그 한 장만 지울 수 있다.
        assertEquals(CARD_ID, navigator.consumeShreddedCardId())
    }

    /** 끝난 대화방이 남으면 뒤로 나갔을 때 이어 쓸 수 없는 방으로 돌아간다. */
    @Test
    fun 버린_카드를_열면_대화방을_걷어내고_보관함_상세만_올린다() {
        val navigator = navigator()
        navigator.navigate(ChatKey)
        navigator.navigate(ChatRoomKey(CONVERSATION_ID))

        navigator.openDroppedCard(ArchiveKey, ArchiveDetailKey(EMOTION), CARD_ID)

        assertEquals(ArchiveDetailKey(EMOTION), navigator.state.currentKey)
        assertEquals(
            listOf(ArchiveKey, ArchiveDetailKey(EMOTION)),
            navigator.state.currentSubStack.toList(),
        )
        assertEquals(listOf(ChatKey), navigator.state.subStacks.getValue(ChatKey).toList())
    }

    /** 신호를 비우지 않으면 그 칸에 다시 들어올 때마다 같은 종이가 또 떨어진다. */
    @Test
    fun 버린_카드_신호는_한_번만_읽힌다() {
        val navigator = navigator()
        navigator.openDroppedCard(ArchiveKey, ArchiveDetailKey(EMOTION), CARD_ID)

        assertEquals(CARD_ID, navigator.consumeDroppedCardId())
        assertEquals(null, navigator.consumeDroppedCardId())
    }

    /** 신호를 비우지 않으면 그 칸에 다시 들어올 때마다 같은 카드를 또 뺀다. */
    @Test
    fun 파쇄_신호는_한_번만_읽힌다() {
        val navigator = navigator()
        navigator.openArchiveDetail()
        navigator.navigate(CardDeleteKey(EMOTION, CARD_ID))
        navigator.finishShreddedCard(CARD_ID)

        navigator.consumeShreddedCardId()

        assertEquals(null, navigator.consumeShreddedCardId())
    }

    private companion object {
        const val CARD_ID = 7L
        const val CONVERSATION_ID = 42L
        val EMOTION = EmotionCharacter.ANGER
    }
}
