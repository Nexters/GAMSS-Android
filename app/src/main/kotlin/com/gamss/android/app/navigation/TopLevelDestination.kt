package com.gamss.android.app.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.gamss.android.app.R
import com.gamss.android.core.designsystem.component.GamssBottomBarItem
import com.gamss.android.core.designsystem.component.GamssIcons
import com.gamss.android.feature.archive.navigation.ArchiveKey
import com.gamss.android.feature.chat.navigation.ChatKey
import com.gamss.android.feature.home.navigation.HomeKey

data class TopLevelDestination(
    val key: NavKey,
    @param:DrawableRes @get:DrawableRes val iconRes: Int,
    @param:StringRes @get:StringRes val labelRes: Int,
)

fun topLevelDestinations(): List<TopLevelDestination> = listOf(
    TopLevelDestination(key = ArchiveKey, iconRes = GamssIcons.TabArchive, labelRes = R.string.tab_archive),
    TopLevelDestination(key = HomeKey, iconRes = GamssIcons.TabHome, labelRes = R.string.tab_home),
    TopLevelDestination(key = ChatKey, iconRes = GamssIcons.TabChat, labelRes = R.string.tab_chat),
)

fun List<TopLevelDestination>.keys(): Set<NavKey> = map { it.key }.toSet()

@Composable
fun List<TopLevelDestination>.bottomBarItems(): List<GamssBottomBarItem<NavKey>> = map { destination ->
    GamssBottomBarItem(
        value = destination.key,
        iconRes = destination.iconRes,
        label = stringResource(destination.labelRes),
    )
}
