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

    /** SDK 조회는 디스크를 기다릴 수 있으므로 [initialize] 에서 한 번만 읽어 둔다. */
    @Volatile
    private var snapshot: Map<RemoteConfigKey, String> = DECLARED_DEFAULTS

    override val isReady: StateFlow<Boolean> = readyState.asStateFlow()

    override suspend fun initialize() {
        if (readyState.value) return
        mutex.withLock {
            if (readyState.value) return
            snapshot = runSafely {
                remote.configure(SDK_DEFAULTS)
                remote.fetchAndActivate()
                readSnapshot()
            } ?: DECLARED_DEFAULTS
            readyState.value = true
        }
    }

    override fun getString(key: RemoteConfigKey): String = snapshot.getValue(key)

    override fun getBoolean(key: RemoteConfigKey): Boolean = snapshot.getValue(key).toBoolean()

    private fun readSnapshot(): Map<RemoteConfigKey, String> =
        RemoteConfigKey.entries.associateWith { remote.read(it.key) ?: it.defaultValue }

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
        val DECLARED_DEFAULTS: Map<RemoteConfigKey, String> =
            RemoteConfigKey.entries.associateWith { it.defaultValue }

        val SDK_DEFAULTS: Map<String, String> = DECLARED_DEFAULTS.mapKeys { it.key.key }
    }
}
