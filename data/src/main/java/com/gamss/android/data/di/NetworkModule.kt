package com.gamss.android.data.di

import android.util.Log
import com.gamss.android.data.BuildConfig
import com.gamss.android.data.auth.AUTHORIZATION_HEADER
import com.gamss.android.data.auth.TokenAuthenticator
import com.gamss.android.data.auth.TokenInterceptor
import com.gamss.android.data.remote.auth.AuthService
import com.gamss.android.data.remote.gamssJson
import com.gamss.android.data.remote.token.TokenUsageService
import com.gamss.android.data.remote.user.UserService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = gamssJson

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor { message ->
            Log.d(HTTP_LOG_TAG, message.redactTokenValues())
        }.apply {
            redactHeader(AUTHORIZATION_HEADER)
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    /**
     * 로그인/토큰 재발급 전용 클라이언트. TokenAuthenticator를 통해 재발급되는 요청과
     * 디스패처·커넥션 풀을 공유하지 않도록 별도로 둔다.
     */
    @Provides
    @Singleton
    @AuthApi
    fun provideAuthOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenInterceptor: TokenInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(tokenInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @AuthApi
    fun provideAuthRetrofit(
        @AuthApi okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(@AuthApi retrofit: Retrofit): AuthService =
        retrofit.create(AuthService::class.java)

    @Provides
    @Singleton
    fun provideUserService(retrofit: Retrofit): UserService =
        retrofit.create(UserService::class.java)

    @Provides
    @Singleton
    fun provideTokenUsageService(retrofit: Retrofit): TokenUsageService =
        retrofit.create(TokenUsageService::class.java)

    private fun String.redactTokenValues(): String =
        TOKEN_JSON_PATTERN.replace(this) { matchResult ->
            "${matchResult.groupValues[1]}<redacted>${matchResult.groupValues[2]}"
        }

    private const val HTTP_LOG_TAG = "GamssHttp"
    private val TOKEN_JSON_PATTERN =
        Regex("(\"(?:idToken|accessToken|refreshToken)\"\\s*:\\s*\")[^\"]*(\")")
}
