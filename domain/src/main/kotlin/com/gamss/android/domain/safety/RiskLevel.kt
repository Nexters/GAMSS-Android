package com.gamss.android.domain.safety

/**
 * CRITICAL 은 전송을 차단하고, WARNING 은 안내만 하고 전송을 진행한다.
 * 등급은 원격 사전이 정하므로 앱 배포 없이 차단 범위를 조정할 수 있다.
 */
enum class RiskLevel {
    NONE,
    WARNING,
    CRITICAL,
}
