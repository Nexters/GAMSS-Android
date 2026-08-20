package com.gamss.android.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import java.time.LocalDate

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
     * key 에 실어 보내지 않는다. key 는 화면의 정체성이라 인자가 남으면 그 화면에 다시 들어올
     * 때마다 방금 버린 것처럼 또 떨어진다. 프로세스가 죽으면 이 값도 사라지는데, 그때는 낙하를
     * 건너뛰는 쪽이 맞다.
     */
    private var droppedCardDate: LocalDate? by mutableStateOf(null)

    fun consumeDroppedCardDate(): LocalDate? = droppedCardDate.also { droppedCardDate = null }

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
     * 방금 버린 카드를 보관함 상세로 데려간다.
     *
     * 순서가 정해져 있다. 현재 탭을 먼저 비워야 끝난 대화방이 남지 않는데, 탭을 옮긴 뒤에는
     * currentSubStack 이 옮겨간 탭을 가리켜 손댈 수 없다. 옮겨간 탭도 root 까지 비우고 [detail]
     * 하나만 올린다. 보관함에 다른 감정 칸이 열려 있었다면 뒤로 나갔을 때 그 칸이 다시 뜬다.
     */
    fun openDroppedCard(topLevel: NavKey, detail: NavKey, droppedCardDate: LocalDate) {
        this.droppedCardDate = droppedCardDate
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
