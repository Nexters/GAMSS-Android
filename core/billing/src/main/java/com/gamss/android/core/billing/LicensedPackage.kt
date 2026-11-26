package com.gamss.android.core.billing

import javax.inject.Qualifier

/** Play 라이선스 테스트가 가능한, Play Console에 등록된 패키지(com.gamss.android)로 설치됐는지. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LicensedPackage
