package com.gamss.android.domain.model

/**
 * 온디바이스 모델(애셋팩) 다운로드 상태. UI 가 이걸 보고 사용자 확인이 필요한 시점에
 * 배너를 띄울지 판단한다.
 *
 * [NEEDS_USER_CONFIRMATION] 은 두 가지 경우를 함께 가리킨다 — 셀룰러로는 자동 진행이 안 돼
 * 확인이 필요한 경우, 그리고 팩이 커서 네트워크 종류와 무관하게 확인이 필요한 경우. 둘 다
 * "사용자가 뭔가 눌러줘야 진행된다"는 점에서 UI 입장에선 동일하게 다뤄도 된다.
 */
enum class ModelDownloadStatus {
    DOWNLOADING,
    NEEDS_USER_CONFIRMATION,
    COMPLETED,
    FAILED,
}
