package com.gamss.android.app.navigation

import com.gamss.android.domain.emotion.EmotionCharacter
import com.gamss.android.feature.archive.navigation.ArchiveDetailKey
import com.gamss.android.feature.archive.navigation.ArchiveKey
import com.gamss.android.feature.carddelete.navigation.CardDeleteKey
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.chat.navigation.ChatRoomKey
import com.gamss.android.feature.home.navigation.HomeKey
import com.gamss.android.feature.login.navigation.LoginKey
import com.gamss.android.feature.onboarding.navigation.OnboardingKey
import com.gamss.android.feature.setting.navigation.AccountInfoKey
import com.gamss.android.feature.setting.navigation.NicknameChangeKey
import com.gamss.android.feature.setting.navigation.SettingKey
import com.gamss.android.feature.webview.GamssWebPage
import com.gamss.android.feature.webview.navigation.WebViewKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenViewTest {

    @Test
    fun `탭을 담는 MainKey 는 화면으로 세지 않는다`() {
        assertNull(MainKey.toScreenView())
    }

    @Test
    fun `실제 화면 key 는 모두 이름이 붙어 있다`() {
        val keys = listOf(
            LoginKey,
            OnboardingKey,
            HomeKey,
            ChatKey,
            ChatRoomKey(1L),
            ArchiveKey,
            ArchiveDetailKey(EmotionCharacter.JOY),
            CardDeleteKey(EmotionCharacter.JOY),
            SettingKey,
            AccountInfoKey,
            NicknameChangeKey("감쓰"),
            WebViewKey(GamssWebPage.ServiceTerms),
        )
        keys.forEach { assertNotNull("$it", it.toScreenView()) }
    }

    @Test
    fun `대화방은 대화 id 를 속성에 넣지 않는다`() {
        assertEquals(ScreenView("chat_room"), ChatRoomKey(1L).toScreenView())
    }

    @Test
    fun `인자가 없는 화면은 이름만 남긴다`() {
        assertEquals(ScreenView("home"), HomeKey.toScreenView())
    }

    @Test
    fun `보관함 상세는 어떤 감정 칸인지 남긴다`() {
        assertEquals(
            ScreenView("archive_detail", mapOf("emotion" to "anger")),
            ArchiveDetailKey(EmotionCharacter.ANGER).toScreenView(),
        )
    }

    @Test
    fun `파쇄 화면은 카드 한 장인지 칸 전체인지 구분한다`() {
        assertEquals(
            ScreenView("card_delete", mapOf("emotion" to "joy", "scope" to "single")),
            CardDeleteKey(EmotionCharacter.JOY, cardId = 1L).toScreenView(),
        )
        assertEquals(
            ScreenView("card_delete", mapOf("emotion" to "joy", "scope" to "emotion_all")),
            CardDeleteKey(EmotionCharacter.JOY).toScreenView(),
        )
    }

    @Test
    fun `닉네임 변경 화면은 현재 닉네임을 속성에 넣지 않는다`() {
        assertEquals(ScreenView("nickname_change"), NicknameChangeKey("감쓰").toScreenView())
    }
}
