package com.gamss.android.domain.payment

/**
 * [UNKNOWN]은 서버가 앱이 모르는 상태값을 보낸 경우다(서버가 상태를 추가했는데 앱이 아직 업데이트되지 않은 경우 등).
 * 정상 응답이라 지급됐을 수도 있으므로, 실패로 안내하지 않고 사용량만 다시 조회한 뒤 다음 복구 때 다시 검증한다.
 */
enum class PaymentStatus { GRANTED, PENDING, UNKNOWN }
