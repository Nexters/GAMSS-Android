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
        runSafely { local.markFetched(System.currentTimeMillis()) }

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
