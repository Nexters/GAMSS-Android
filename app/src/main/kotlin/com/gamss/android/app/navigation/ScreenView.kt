package com.gamss.android.app.navigation

import androidx.navigation3.runtime.NavKey
import com.gamss.android.domain.analytics.AnalyticsProperty
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
import com.gamss.android.feature.webview.navigation.WebViewKey

const val SCREEN_VIEW_EVENT = "screen_view"

data class ScreenView(
    val name: String,
    val properties: Map<String, Any> = emptyMap(),
)

// 인자 없는 화면은 Map 으로 둔다. when 분기에 두면 화면이 늘 때마다 복잡도가 올라 detekt
// CyclomaticComplexMethod 검사에서 실패한다.
private val screenNames: Map<NavKey, String> = mapOf(
    LoginKey to "login",
    OnboardingKey to "onboarding",
    HomeKey to "home",
    ChatKey to "chat_list",
    ArchiveKey to "archive",
    SettingKey to "setting",
    AccountInfoKey to "account_info",
)

/** 새 화면 key 를 추가하면 여기에도 이름을 붙여야 집계된다. MainKey 는 탭을 담는 틀이라 세지 않는다. */
fun NavKey.toScreenView(): ScreenView? = screenNames[this]?.let(::ScreenView) ?: when (this) {
    is ChatRoomKey -> ScreenView("chat_room")
    is ArchiveDetailKey -> ScreenView("archive_detail", mapOf(AnalyticsProperty.EMOTION to emotion.name.lowercase()))
    is CardDeleteKey -> ScreenView(
        name = "card_delete",
        properties = mapOf(
            AnalyticsProperty.EMOTION to emotion.name.lowercase(),
            AnalyticsProperty.SCOPE to if (cardId == null) "emotion_all" else "single",
        ),
    )
    is NicknameChangeKey -> ScreenView("nickname_change")
    is WebViewKey -> ScreenView("webview", mapOf("page" to page.name.lowercase()))
    else -> null
}
