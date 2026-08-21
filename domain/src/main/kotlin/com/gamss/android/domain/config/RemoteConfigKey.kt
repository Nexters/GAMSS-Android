package com.gamss.android.domain.config

/**
 * 원격 설정 키와 기본값의 단일 출처.
 *
 * 기능 플래그의 기본값은 꺼짐이다. 원격 설정을 받지 못한 상태에서 기능이 열리면 안 된다.
 */
enum class RemoteConfigKey(val key: String, val defaultValue: String) {
    /** 채팅방의 카드 생성 버튼 노출을 제어했던 키. 지금은 읽는 곳이 없지만 서버에 등록돼 있어 남겨 둔다. */
    UseChatEndFeature("use_chat_end_feature", "false"),
}
