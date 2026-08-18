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
