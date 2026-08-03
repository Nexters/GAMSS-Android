package com.gamss.android.data.safety

import com.gamss.android.data.safety.model.RiskLexiconDto
import com.gamss.android.data.safety.model.toDomain
import com.gamss.android.domain.safety.RiskLexicon
import com.gamss.android.domain.safety.RiskLexiconRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

/**
 * 내장 사전과 원격 사전 중 version 이 큰 쪽을 쓴다. 원격 조회나 캐시 파싱이 실패해도
 * 내장 사전으로 계속 동작해야 하므로 이 클래스 밖으로 예외를 내보내지 않는다.
 */
@Singleton
internal class RiskLexiconRepositoryImpl @Inject constructor(
    private val bundled: BundledRiskLexiconDataSource,
    private val remote: FirestoreRiskLexiconDataSource,
    private val local: RiskLexiconLocalDataSource,
    private val json: Json,
) : RiskLexiconRepository {

    private val mutex = Mutex()
    private var cached: RiskLexicon? = null

    override suspend fun getLexicon(): RiskLexicon = mutex.withLock { loadOnce() }

    override suspend fun refresh() {
        if (isCacheStale()) {
            fetchRemoteOrNull()?.let { adopt(it) }
        }
    }

    private suspend fun adopt(fetched: RiskLexiconDto) {
        // 채택 여부와 무관하게 조회 시각을 남긴다. 남기지 않으면 TTL 이 갱신되지 않아 앱 실행마다 다시 조회한다.
        runSafely { local.markFetched(System.currentTimeMillis()) }

        // 필드명이 바뀌거나 파싱이 어긋나면 빈 사전이 온다. 그대로 채택하면 감지가 통째로 꺼진다.
        if (fetched.terms.isEmpty() || fetched.agencies.isEmpty()) return

        mutex.withLock {
            if (fetched.version > loadOnce().version) {
                writeCache(fetched)
                cached = fetched.toDomain()
            }
        }
    }

    private suspend fun loadOnce(): RiskLexicon = cached ?: load().also { cached = it }

    private suspend fun load(): RiskLexicon {
        val candidates = listOfNotNull(loadBundledOrNull(), loadStoredOrNull())
        return candidates.maxByOrNull { it.version }?.toDomain() ?: EMPTY
    }

    private suspend fun isCacheStale(): Boolean {
        val fetchedAt = readCacheOrNull()?.fetchedAtMillis ?: return true
        return System.currentTimeMillis() - fetchedAt >= CACHE_TTL_MILLIS
    }

    private suspend fun writeCache(dto: RiskLexiconDto) {
        runSafely { local.write(json.encodeToString(dto), System.currentTimeMillis()) }
    }

    private suspend fun readCacheOrNull(): CachedRiskLexicon? = runSafely { local.read() }

    private suspend fun loadStoredOrNull(): RiskLexiconDto? = runSafely {
        readCacheOrNull()?.json?.let { json.decodeFromString<RiskLexiconDto>(it) }
    }

    private suspend fun loadBundledOrNull(): RiskLexiconDto? = runSafely { bundled.load() }

    private suspend fun fetchRemoteOrNull(): RiskLexiconDto? = runSafely { remote.fetch() }

    /**
     * 사전 로딩 실패가 감지 실패로 이어지지 않도록 삼킨다. 취소만 그대로 전파한다.
     */
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
        val EMPTY = RiskLexicon(
            version = 0,
            terms = emptyList(),
            safePhrases = emptyList(),
            agencies = emptyList(),
        )
        val CACHE_TTL_MILLIS = 24.hours.inWholeMilliseconds
    }
}
