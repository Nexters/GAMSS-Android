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
     * key 에 실어 보내지 않는다. key 는 화면의 정체성이라 인자가 남으면 그 화면에 다시 들어올
     * 때마다 방금 버린 것처럼 또 떨어진다. 프로세스가 죽으면 이 값도 사라지는데, 그때는 낙하를
     * 건너뛰는 쪽이 맞다.
     */
    private var droppedCardId: Long? = null

    fun consumeDroppedCardId(): Long? = droppedCardId.also { droppedCardId = null }

    /** 위 카드 id 와 같은 이유로 key 에 싣지 않는다. */
    private var shreddedCardId: Long? = null

    fun consumeShreddedCardId(): Long? = shreddedCardId.also { shreddedCardId = null }

    /** top-level key인지, 현재 탭인지, 상세 key인지에 따라 stack 갱신 규칙이 달라진다. */
    fun navigate(key: NavKey) {
        when (key) {
            state.currentTopLevelKey -> clearSubStack()
            in state.topLevelKeys -> goToTopLevel(key)
            else -> goToKey(key)
        }
    }

    /**
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

    fun finishCurrentFlow() {
        clearSubStack()
    }

    /**
     * 카드 한 장을 파쇄하고 원래 보던 칸으로 돌아간다.
     *
     * 남은 종이는 그대로 보여야 하므로 [finishCurrentFlow] 처럼 보관함 첫 화면까지 걷어내지 않는다.
     * 대신 그 칸의 ViewModel 이 살아남아 지운 카드를 그대로 들고 있으므로, 어느 카드였는지를
     * 남긴다. 파쇄가 끝났다는 건 서버에서 이미 지워졌다는 뜻이라, 그 한 장만 빼면 목록이 서버와
     * 같아진다. 달·감정이 그대로인 목록을 통째로 다시 받을 이유가 없다.
     */
    fun finishShreddedCard(cardId: Long) {
        shreddedCardId = cardId
        goBack()
    }

    /**
     * 방금 버린 카드를 보관함 상세로 데려간다.
     *
     * 순서가 정해져 있다. 현재 탭을 먼저 비워야 끝난 대화방이 남지 않는데, 탭을 옮긴 뒤에는
     * currentSubStack 이 옮겨간 탭을 가리켜 손댈 수 없다. 옮겨간 탭도 root 까지 비우고 [detail]
     * 하나만 올린다. 보관함에 다른 감정 칸이 열려 있었다면 뒤로 나갔을 때 그 칸이 다시 뜬다.
     */
    fun openDroppedCard(topLevel: NavKey, detail: NavKey, droppedCardId: Long) {
        this.droppedCardId = droppedCardId
        clearSubStack()
        goToTopLevel(topLevel)
        clearSubStack()
        goToKey(detail)
    }

    private fun goToKey(key: NavKey) {
        state.currentSubStack.apply {
            remove(key)
            add(key)
        }
    }

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

    private fun clearSubStack() {
        state.currentSubStack.run {
            if (size > 1) subList(1, size).clear()
        }
    }
}
