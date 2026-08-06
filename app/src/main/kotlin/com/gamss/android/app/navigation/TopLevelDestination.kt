package com.gamss.android.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.gamss.android.core.ui.GamssBottomBarItem
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
    val icon: ImageVector,
    val label: String,
)

fun topLevelDestinations(isDebug: Boolean): List<TopLevelDestination> = buildList {
    add(TopLevelDestination(key = HomeKey, icon = Icons.Filled.Home, label = "홈"))
    add(TopLevelDestination(key = CalendarKey, icon = Icons.Filled.DateRange, label = "달력"))
    add(TopLevelDestination(key = EmotionKey, icon = Icons.Filled.Favorite, label = "감정"))
    if (isDebug) {
        add(TopLevelDestination(key = ChatKey, icon = Icons.Filled.Email, label = "대화"))
    }
}

fun List<TopLevelDestination>.keys(): Set<NavKey> = map { it.key }.toSet()

fun List<TopLevelDestination>.bottomBarItems(): List<GamssBottomBarItem<NavKey>> = map { destination ->
    GamssBottomBarItem(
        value = destination.key,
        icon = destination.icon,
        label = destination.label,
    )
}
