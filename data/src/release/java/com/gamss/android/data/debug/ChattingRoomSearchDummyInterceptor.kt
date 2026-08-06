package com.gamss.android.data.debug

import okhttp3.Interceptor

/**
 * 릴리즈 빌드에서는 더미 응답을 사용하지 않고 항상 실제 네트워크로 그대로 통과시킨다.
 * debug 빌드 전용 구현은 `data/src/debug`의 동일 패키지/시그니처 파일을 본다.
 */
internal fun provideChattingRoomSearchDummyInterceptor(): Interceptor = Interceptor { chain ->
    chain.proceed(chain.request())
}
