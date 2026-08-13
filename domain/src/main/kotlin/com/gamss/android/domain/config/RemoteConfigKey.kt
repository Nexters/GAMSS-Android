package com.gamss.android.domain.config

/**
 * 원격 설정 키와 기본값의 단일 출처.
 *
 * 기능 플래그의 기본값은 꺼짐이다. 원격 설정을 받지 못한 상태에서 기능이 열리면 안 된다.
 */
enum class RemoteConfigKey(val key: String, val defaultValue: String) {
    /**
     * 카드 기능 전체를 연다. 대화 끝내기 버튼과 보관함 탭을 함께 제어한다.
     *
     * 카드는 종료된 대화에만 생기므로 둘을 따로 두면 앞뒤가 안 맞는다. 끝내기만 막으면 보관함이
     * 영영 비고, 보관함만 막으면 쌓인 카드를 볼 곳이 없다.
     */
    UseChatEndFeature("use_chat_end_feature", "false"),
}
