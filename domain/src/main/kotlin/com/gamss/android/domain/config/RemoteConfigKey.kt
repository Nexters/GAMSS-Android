package com.gamss.android.domain.config

/**
 * 원격 설정 키와 기본값의 단일 출처.
 *
 * 기능 플래그의 기본값은 꺼짐이다. 원격 설정을 받지 못한 상태에서 기능이 열리면 안 된다.
 */
enum class RemoteConfigKey(val key: String, val defaultValue: String) {
    UseCardFeature("use_card_feature", "false"),
}
