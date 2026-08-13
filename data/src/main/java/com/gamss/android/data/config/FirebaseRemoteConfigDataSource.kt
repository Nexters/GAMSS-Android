package com.gamss.android.data.config

import com.gamss.android.core.common.BuildInfo
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

@Singleton
internal class FirebaseRemoteConfigDataSource @Inject constructor(
    private val remoteConfigProvider: Provider<FirebaseRemoteConfig>,
    private val buildInfo: BuildInfo,
) {
    suspend fun configure(defaults: Map<String, String>) {
        val remoteConfig = remoteConfigProvider.get()
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = fetchIntervalSeconds()
                fetchTimeoutInSeconds = FETCH_TIMEOUT_SECONDS
            },
        ).await()
        remoteConfig.setDefaultsAsync(defaults).await()
    }

    suspend fun fetchAndActivate(): Boolean = remoteConfigProvider.get().fetchAndActivate().await()

    /** 원격 값도 등록된 기본값도 없으면 null. 첫 호출은 디스크 캐시를 기다릴 수 있다. */
    fun read(key: String): String? {
        val value = remoteConfigProvider.get().getValue(key)
        return value.asString().takeIf { value.source != FirebaseRemoteConfig.VALUE_SOURCE_STATIC }
    }

    /** 디버그 빌드는 콘솔 변경을 바로 확인해야 하므로 조회 간격 제한을 두지 않는다. */
    private fun fetchIntervalSeconds(): Long =
        if (buildInfo.isDebug) 0L else RELEASE_FETCH_INTERVAL_SECONDS

    private companion object {
        val RELEASE_FETCH_INTERVAL_SECONDS = 12.hours.inWholeSeconds

        /** 게이트가 풀린 뒤에도 fetch 는 계속 진행하되, SDK 기본값 60초까지 끌지는 않는다. */
        const val FETCH_TIMEOUT_SECONDS = 10L
    }
}
