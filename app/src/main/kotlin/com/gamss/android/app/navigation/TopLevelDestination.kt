package com.gamss.android.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.gamss.android.app.R
import com.gamss.android.core.designsystem.component.GamssBottomBarItem
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.feature.calendar.navigation.CalendarKey
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.emotion.navigation.EmotionKey
import com.gamss.android.feature.home.navigation.HomeKey

/**
 * bottom bar에 표시되는 최상위 탭 목록.
 *
 * 새로운 feature 모듈이 하단 탭으로 추가될 때마다 이 목록에 항목을 더한다.
 */
data class TopLevelDestination(
    val key: NavKey,
    @param:DrawableRes @get:DrawableRes val iconRes: Int,
    @param:StringRes @get:StringRes val labelRes: Int,
)

fun topLevelDestinations(isDebug: Boolean): List<TopLevelDestination> = buildList {
    add(TopLevelDestination(key = CalendarKey, iconRes = GamssIcons.TabArchive, labelRes = R.string.tab_archive))
    add(TopLevelDestination(key = HomeKey, iconRes = GamssIcons.TabHome, labelRes = R.string.tab_home))
    add(TopLevelDestination(key = ChatKey, iconRes = GamssIcons.TabChat, labelRes = R.string.tab_chat))
    if (isDebug) {
        add(TopLevelDestination(key = EmotionKey, iconRes = R.drawable.ic_tab_debug, labelRes = R.string.tab_emotion))
    }
}

fun List<TopLevelDestination>.keys(): Set<NavKey> = map { it.key }.toSet()

@Composable
fun List<TopLevelDestination>.bottomBarItems(): List<GamssBottomBarItem<NavKey>> = map { destination ->
    GamssBottomBarItem(
        value = destination.key,
        iconRes = destination.iconRes,
        label = stringResource(destination.labelRes),
    )
}
