package com.gamss.android.core.common

interface BuildInfo {

    /** HTTP 본문 로그처럼 디버깅용 출력을 켤지. */
    val isDebug: Boolean

    /** 아직 정식 노출 전인 화면을 열지. 팀에 돌리는 내부 배포본에서도 켠다. */
    val showsInternalTools: Boolean
}
