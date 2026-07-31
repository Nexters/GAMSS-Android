package com.gamss.android.data.di

import com.gamss.android.data.BuildConfig
import com.gamss.android.data.auth.DevTokenInterceptor
import com.gamss.android.data.remote.auth.AuthService
import com.gamss.android.data.remote.card.CardService
import com.gamss.android.data.remote.conversation.ConversationService
import com.gamss.android.data.remote.gamssJson
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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = gamssJson

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            redactHeader(AUTHORIZATION_HEADER)
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            // TODO(google-login 머지 시 삭제): TokenInterceptor·TokenAuthenticator 로 대체한다.
            .addInterceptor(DevTokenInterceptor())
            .addInterceptor(loggingInterceptor)
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
    fun provideAuthService(retrofit: Retrofit): AuthService =
        retrofit.create(AuthService::class.java)

    @Provides
    @Singleton
    fun provideConversationService(retrofit: Retrofit): ConversationService =
        retrofit.create(ConversationService::class.java)

    @Provides
    @Singleton
    fun provideCardService(retrofit: Retrofit): CardService =
        retrofit.create(CardService::class.java)

    private const val AUTHORIZATION_HEADER = "Authorization"
}
