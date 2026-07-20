package com.gamss.android.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * GAMSS 메인 탭 네비게이션 상태를 생성한다.
 *
 * Navigation3의 back stack은 rememberSaveable 기반으로 동작하므로 화면 회전이나
 * 프로세스 재생성 이후에도 복원 가능한 key를 사용해야 한다.
 *
 * @param startKey 앱의 메인 영역에서 처음 보여줄 최상위 key
 * @param topLevelKeys bottom bar에 연결되는 최상위 key 목록
 */
@Composable
fun rememberNavigationState(
    startKey: NavKey,
    topLevelKeys: Set<NavKey>,
): NavigationState {
    val topLevelStack = rememberNavBackStack(startKey)
    val subStacks = topLevelKeys.associateWith { key -> rememberNavBackStack(key) }

    return remember(startKey, topLevelKeys) {
        NavigationState(
            startKey = startKey,
            topLevelStack = topLevelStack,
            subStacks = subStacks,
        )
    }
}

/**
 * bottom bar 기반 화면 전환을 위한 상태 홀더.
 *
 * topLevelStack은 사용자가 방문한 탭 순서를 저장하고, subStacks는 각 탭 내부의 상세 화면
 * stack을 따로 저장한다. 탭을 전환해도 탭의 상세 화면 흐름을 유지할 수 있다.
 */
class NavigationState(
    val startKey: NavKey,
    val topLevelStack: NavBackStack<NavKey>,
    val subStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    val currentTopLevelKey: NavKey by derivedStateOf { topLevelStack.last() }

    val topLevelKeys
        get() = subStacks.keys

    val currentSubStack: NavBackStack<NavKey>
        get() = subStacks[currentTopLevelKey]
            ?: error("현재 탭($currentTopLevelKey)에 해당하는 back stack이 없습니다.")

    val currentKey: NavKey by derivedStateOf { currentSubStack.last() }
}

/**
 * NavigationState를 NavDisplay에서 사용할 entry 목록으로 변환한다.
 *
 * 각 탭의 sub stack에 saveable state와 ViewModelStore decorator를 붙여서, 탭 전환 중에도
 * 화면 상태와 ViewModel 생명주기가 탭별 back stack에 맞게 유지되도록 한다.
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> {
    val decoratedEntries = subStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider,
        )
    }

    return topLevelStack
        .flatMap { decoratedEntries[it] ?: emptyList() }
}
