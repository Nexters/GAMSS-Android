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
import com.gamss.android.feature.home.navigation.HomeKey

data class TopLevelDestination(
    val key: NavKey,
    @param:DrawableRes @get:DrawableRes val iconRes: Int,
    @param:StringRes @get:StringRes val labelRes: Int,
    /** 원격 설정 `use_card_feature` 가 켜졌을 때만 탭바에 노출한다. */
    val requiresCardFeature: Boolean = false,
)

/**
 * 이 목록은 원격 설정에 좌우되면 안 된다.
 *
 * [rememberNavigationState] 가 key 마다 `rememberNavBackStack` 을 루프로 부르는데, 저장 키가 호출 순번으로
 * 정해져서 개수나 순서가 실행마다 달라지면 프로세스 재생성 뒤 다른 탭의 백스택이 복원된다.
 * 노출 여부는 [visibleIn] 으로 탭바에서만 거른다.
 */
fun topLevelDestinations(): List<TopLevelDestination> = listOf(
    TopLevelDestination(
        key = CalendarKey,
        iconRes = GamssIcons.TabArchive,
        labelRes = R.string.tab_archive,
        requiresCardFeature = true,
    ),
    TopLevelDestination(key = HomeKey, iconRes = GamssIcons.TabHome, labelRes = R.string.tab_home),
    TopLevelDestination(key = ChatKey, iconRes = GamssIcons.TabChat, labelRes = R.string.tab_chat),
)

fun List<TopLevelDestination>.keys(): Set<NavKey> = map { it.key }.toSet()

fun List<TopLevelDestination>.visibleIn(useCardFeature: Boolean): List<TopLevelDestination> =
    filter { destination -> useCardFeature || !destination.requiresCardFeature }

@Composable
fun List<TopLevelDestination>.bottomBarItems(): List<GamssBottomBarItem<NavKey>> = map { destination ->
    GamssBottomBarItem(
        value = destination.key,
        iconRes = destination.iconRes,
        label = stringResource(destination.labelRes),
    )
}
