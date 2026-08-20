package com.gamss.android.feature.archive

import androidx.compose.ui.unit.dp

// 아래 상수는 대부분 실기기(갤럭시 S23)로 직접 눈으로 보면서 맞춘 경험적 튜닝값이다.
// 단위는 px/s, px/s², rad/s 등 시뮬레이션 내부 단위 기준이며, 다른 값으로 바꾸면 반드시
// 실기기에서 낙하 애니메이션을 재확인해야 한다.

// --- 시안 치수 ---

/** 종이 한 장의 크기. 시안 폭 기준이라 좁은 기기에서는 함께 줄어든다. */
internal val PaperSize = 88.dp

/** 더미가 시작하는 높이. 위쪽 상단바와 월 셀렉터를 피한다. */
internal val PaperPileTopPadding = 136.dp

/** 종이는 네모지만 충돌은 원으로 근사한다. 모서리까지 덮으려 반지름을 조금 키운다. */
internal const val PAPER_COLLISION_RADIUS_SCALE = 1.15f

// --- 쏟아지는 모습 ---

/** 흩뿌릴 때 줄 수 있는 기울기. 0 을 중심으로 ± 이 값까지다. */
internal const val PAPER_MAX_TILT_DEGREES = 42f

/** 뒤 순번일수록 이만큼씩 더 높이 둔다. 한꺼번에 떨어지면 쌓이는 모습이 안 보인다. */
internal const val PAPER_SPAWN_STAGGER = 0.9f

/**
 * 낙하가 너무 반듯해 보이지 않게 출발할 때 옆으로 밀고 살짝 돌려 둔다.
 * 아래 셋 모두 0 을 중심으로 ± 이 값까지의 범위다.
 */
internal const val PAPER_SPAWN_DRIFT = 60f
internal const val PAPER_SPAWN_SPIN = 0.075f

// --- 시뮬레이션 진행 ---

/** 프레임 기준이 없을 때 쓰는 한 프레임치 간격. 미리 굴릴 때도 이 간격으로 돈다. */
internal const val PAPER_FIXED_DT = 1f / 60f

/** 프레임이 밀렸을 때 위로 자르는 간격. 안 자르면 한 프레임에 종이가 벽을 뚫는다. */
internal const val PAPER_MAX_DT = 1f / 30f

/** 이만큼 연속으로 잠잠하면 다 쌓인 것으로 본다. */
internal const val PAPER_SETTLE_FRAMES = 30

/** 잠잠해지지 않아도 여기서 멈춘다. 화면이 그대로인데 매 프레임 깨어나지 않게 한다. */
internal const val PAPER_MAX_DURATION_NANOS = 5_000_000_000L

/** 미리 굴릴 때의 상한. 10초치라 실제로는 훨씬 먼저 잠잠해진다. */
internal const val PAPER_SETTLE_MAX_STEPS = 600

// --- 물리 ---

/** 이보다 느리게 움직이면 멈춘 것으로 본다. 종이 지름의 3분의 1도 1초에 못 가는 속도다. */
internal const val PAPER_REST_SPEED = 30f

internal const val PAPER_GRAVITY = 2600f
internal const val PAPER_LINEAR_DAMPING = 0.995f
internal const val PAPER_ANGULAR_DAMPING = 0.9f
internal const val PAPER_RESTITUTION = 0.32f
internal const val PAPER_FRICTION = 0.9f
internal const val PAPER_COLLISION_ITERATIONS = 4

/** 종이 무게는 모두 같다고 보므로 충격량과 겹침을 양쪽이 반씩 나눈다. */
internal const val PAPER_EQUAL_MASS_SHARE = 0.5f

// 회전 전달은 일부러 아주 작게 둔다. 종이는 처음 기울기를 대체로 유지하고 부딪힐 때만 살짝 흔들려야 한다.
internal const val PAPER_PAIR_SPIN_TRANSFER = 0.00012f
internal const val PAPER_WALL_SPIN_TRANSFER = 0.0001f
internal const val PAPER_FLOOR_SPIN_TRANSFER = 0.0002f
internal const val PAPER_MAX_ANGULAR_VELOCITY = 0.8f
internal const val PAPER_MIN_SEPARATION_DISTANCE = 1e-4f
internal const val PAPER_ANGULAR_SLEEP_THRESHOLD = 0.05f
internal const val PAPER_RESTING_TANGENT_SPEED = 8f
internal const val PAPER_POSITION_SLOP = 0.5f
