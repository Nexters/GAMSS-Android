package com.gamss.android.data.config

import com.gamss.android.domain.config.RemoteConfigKey
import com.gamss.android.domain.config.RemoteConfigRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RemoteConfigRepositoryImpl @Inject constructor(
    private val remote: FirebaseRemoteConfigDataSource,
) : RemoteConfigRepository {

    private val mutex = Mutex()
    private val readyState = MutableStateFlow(false)

    override val isReady: StateFlow<Boolean> = readyState.asStateFlow()

    override suspend fun initialize() {
        if (readyState.value) return
        mutex.withLock {
            if (readyState.value) return
            runSafely {
                remote.configure(DEFAULTS)
                remote.fetchAndActivate()
            }
            readyState.value = true
        }
    }

    override fun getString(key: RemoteConfigKey): String =
        if (remote.hasValue(key.key)) remote.getString(key.key) else key.defaultValue

    override fun getBoolean(key: RemoteConfigKey): Boolean =
        if (remote.hasValue(key.key)) remote.getBoolean(key.key) else key.defaultValue.toBoolean()

    /** 원격 설정 실패가 앱 시작을 막으면 안 되므로 취소를 제외한 모든 예외를 삼킨다. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun <T> runSafely(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    private companion object {
        val DEFAULTS: Map<String, String> =
            RemoteConfigKey.entries.associate { it.key to it.defaultValue }
    }
}
