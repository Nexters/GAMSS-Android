package com.gamss.android.feature.carddelete

/** 파쇄하기를 몇 번 눌러야 끝까지 내려가는지와, 한 번 누를 때마다 종이가 움직이는 시간. */
internal const val SHRED_TOTAL_TAPS = 6
internal const val SHRED_TAP_STEP_DURATION_MS = 220

/** 전체 진행도 중 찢기(위쪽 고정, 높이만 자람) 구간이 차지하는 비율. 나머지는 낙하 구간이다. */
internal const val SHRED_TEAR_FRACTION = 0.3f

/** 삭제 요청과 병행하되, 다 내려간 모습을 최소로 유지하는 시간. */
internal const val SHRED_COMPLETE_HOLD_MS = 500L
