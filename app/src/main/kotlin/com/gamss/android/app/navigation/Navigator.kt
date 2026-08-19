package com.gamss.android.app.navigation

import androidx.navigation3.runtime.NavKey

/**
 * NavigationState를 변경하는 앱 전용 navigator.
 *
 * GAMSS의 bottom bar 정책을 한 곳에 모아둔다.
 * - 현재 탭을 다시 선택하면 해당 탭의 root 화면으로 이동
 * - 다른 탭을 선택하면 방문한 탭 순서를 topLevelStack에 기록
 * - 상세 화면은 현재 탭의 sub stack에 쌓되, 같은 key가 이미 있으면 마지막으로 이동
 */
class Navigator(val state: NavigationState) {

    /**
     * 지정한 key로 이동한다.
     * top-level key인지, 현재 탭인지, 상세 key인지에 따라 stack 갱신 규칙이 달라진다.
     */
    fun navigate(key: NavKey) {
        when (key) {
            state.currentTopLevelKey -> clearSubStack()
            in state.topLevelKeys -> goToTopLevel(key)
            else -> goToKey(key)
        }
    }

    /**
     * 현재 위치에서 뒤로 이동한다.
     *
     * 현재 탭의 root 화면에서는 이전에 방문한 탭으로 돌아가고, 상세 화면에서는 현재 탭의
     * sub stack에서 한 단계 pop한다.
     */
    fun goBack() {
        when (state.currentKey) {
            state.startKey -> Unit
            state.currentTopLevelKey -> {
                state.topLevelStack.removeLastOrNull()
            }
            else -> state.currentSubStack.removeLastOrNull()
        }
    }

    /** 완료된 상세 흐름을 닫고 현재 탭의 첫 화면으로 돌아간다. */
    fun finishCurrentFlow() {
        clearSubStack()
    }

    /**
     * 지금 흐름을 끝내고 다른 탭의 상세 화면으로 건너간다.
     *
     * 순서가 정해져 있다. 현재 탭을 먼저 비워야 끝난 화면이 남지 않는데, 탭을 옮긴 뒤에는
     * currentSubStack 이 옮겨간 탭을 가리켜 손댈 수 없다. 옮겨간 탭도 root 까지 비우고 [detail]
     * 하나만 올린다 — 같은 화면이라도 인자가 다르면 다른 key 라, 남겨 두면 같은 화면이 두 장
     * 쌓이고 뒤로 나갔을 때 인자가 없는 쪽이 다시 뜬다.
     */
    fun openInTab(topLevel: NavKey, detail: NavKey) {
        clearSubStack()
        goToTopLevel(topLevel)
        clearSubStack()
        goToKey(detail)
    }

    /**
     * 현재 탭의 상세 화면으로 이동한다.
     */
    private fun goToKey(key: NavKey) {
        state.currentSubStack.apply {
            remove(key)
            add(key)
        }
    }

    /**
     * 다른 top-level 탭으로 이동한다.
     */
    private fun goToTopLevel(key: NavKey) {
        state.topLevelStack.apply {
            if (key == state.startKey) {
                clear()
            } else {
                remove(key)
            }
            add(key)
        }
    }

    /**
     * 현재 탭을 다시 선택했을 때 root 화면만 남긴다.
     */
    private fun clearSubStack() {
        state.currentSubStack.run {
            if (size > 1) subList(1, size).clear()
        }
    }
}
