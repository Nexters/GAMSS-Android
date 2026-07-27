package com.gamss.android.data.di

import javax.inject.Qualifier

/**
 * 로그인/토큰 재발급 전용 OkHttpClient·Retrofit·AuthService를 가리킨다.
 * 인증이 필요한 일반 API용 클라이언트와 디스패처·커넥션 풀을 분리해,
 * 401 폭주 시 재발급 요청이 같은 풀에서 대기하며 발생하는 교착 상태를 막기 위함이다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class AuthNetwork
