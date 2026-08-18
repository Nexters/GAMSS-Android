package com.gamss.android.domain.config

/**
 * 원격 설정 키와 기본값의 단일 출처.
 *
 * 기능 플래그의 기본값은 꺼짐이다. 원격 설정을 받지 못한 상태에서 기능이 열리면 안 된다.
 */
enum class RemoteConfigKey(val key: String, val defaultValue: String) {
    /** 채팅방의 "대화 끝내기" 버튼 노출을 제어한다. 보관함 탭은 원격 설정과 무관하게 항상 노출된다. */
    UseChatEndFeature("use_chat_end_feature", "false"),
}
