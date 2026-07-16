package com.gamss.android.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.gamss.android.core.ui.GamssBottomBar
import com.gamss.android.core.ui.GamssBottomBarItem
import com.gamss.android.feature.home.HomeScreen
import com.gamss.android.feature.home.navigation.HomeKey

@Composable
fun GamssNavHost() {
    val navigationState = rememberNavigationState(
        startKey = HomeKey,
        topLevelKeys = topLevelDestinations.map { it.key }.toSet(),
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    Scaffold(
        bottomBar = {
            GamssBottomBar(
                items = topLevelDestinations.map { destination ->
                    GamssBottomBarItem(
                        icon = destination.icon,
                        label = destination.label,
                        selected = destination.key == navigationState.currentTopLevelKey,
                        onClick = { navigator.navigate(destination.key) },
                    )
                },
            )
        },
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            entries = navigationState.toEntries(
                entryProvider = entryProvider {
                    entry<HomeKey> { HomeScreen() }
                },
            ),
            onBack = navigator::goBack,
        )
    }
}
