package com.gamss.android.data.config

import com.gamss.android.domain.config.RemoteConfigKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class RemoteConfigRepositoryImplTest {

    private lateinit var remote: FirebaseRemoteConfigDataSource

    @Before
    fun setUp() {
        remote = mockk()
    }

    private fun repository() = RemoteConfigRepositoryImpl(remote)

    private fun stubFetchSuccess(value: String) {
        coEvery { remote.configure(any()) } returns Unit
        coEvery { remote.fetchAndActivate() } returns true
        every { remote.read(any()) } returns value
    }

    @Test
    fun `원격 조회가 실패해도 준비 완료로 표시한다`() = runTest {
        coEvery { remote.configure(any()) } returns Unit
        coEvery { remote.fetchAndActivate() } throws IOException("network down")

        val repository = repository()
        repository.initialize()

        assertTrue(repository.isReady.value)
    }

    @Test
    fun `기본값 등록이 실패해도 준비 완료로 표시한다`() = runTest {
        coEvery { remote.configure(any()) } throws IOException("play services unavailable")

        val repository = repository()
        repository.initialize()

        assertTrue(repository.isReady.value)
        coVerify(exactly = 0) { remote.fetchAndActivate() }
    }

    @Test
    fun `initialize 를 두 번 호출해도 원격 조회는 한 번만 수행한다`() = runTest {
        stubFetchSuccess("false")

        val repository = repository()
        repository.initialize()
        repository.initialize()

        coVerify(exactly = 1) { remote.fetchAndActivate() }
    }

    @Test
    fun `모든 설정 키의 기본값을 등록한다`() = runTest {
        val defaults = slot<Map<String, String>>()
        coEvery { remote.configure(capture(defaults)) } returns Unit
        coEvery { remote.fetchAndActivate() } returns true
        every { remote.read(any()) } returns null

        repository().initialize()

        assertEquals(RemoteConfigKey.entries.size, defaults.captured.size)
        RemoteConfigKey.entries.forEach { key ->
            assertEquals(key.defaultValue, defaults.captured[key.key])
        }
    }

    @Test
    fun `취소 예외는 삼키지 않고 준비 상태도 바꾸지 않는다`() {
        coEvery { remote.configure(any()) } throws CancellationException("cancelled")
        val repository = repository()

        assertThrows(CancellationException::class.java) {
            runBlocking { repository.initialize() }
        }

        assertFalse(repository.isReady.value)
    }

    @Test
    fun `동시에 initialize 를 호출해도 원격 조회는 한 번만 수행한다`() = runTest {
        val configureGate = CompletableDeferred<Unit>()
        coEvery { remote.configure(any()) } coAnswers { configureGate.await() }
        coEvery { remote.fetchAndActivate() } returns true
        every { remote.read(any()) } returns "false"

        val repository = repository()
        val first = launch { repository.initialize() }
        val second = launch { repository.initialize() }
        runCurrent()
        configureGate.complete(Unit)
        first.join()
        second.join()

        coVerify(exactly = 1) { remote.configure(any()) }
        coVerify(exactly = 1) { remote.fetchAndActivate() }
    }

    @Test
    fun `initialize 이전에는 준비 상태가 아니다`() {
        assertFalse(repository().isReady.value)
    }

    @Test
    fun `원격 값이 있으면 그 값을 돌려준다`() = runTest {
        stubFetchSuccess("true")

        val repository = repository()
        repository.initialize()

        assertTrue(repository.getBoolean(RemoteConfigKey.UseCardFeature))
    }

    @Test
    fun `조회는 initialize 이후 SDK 를 다시 호출하지 않는다`() = runTest {
        stubFetchSuccess("true")

        val repository = repository()
        repository.initialize()
        repeat(3) { repository.getBoolean(RemoteConfigKey.UseCardFeature) }

        RemoteConfigKey.entries.forEach { key ->
            verify(exactly = 1) { remote.read(key.key) }
        }
    }

    @Test
    fun `initialize 이전 조회도 선언한 기본값을 돌려준다`() {
        val key = RemoteConfigKey.UseCardFeature

        val repository = repository()

        assertEquals(key.defaultValue, repository.getString(key))
        assertFalse(repository.getBoolean(key))
    }

    @Test
    fun `원격 조회가 실패하면 선언한 기본값을 돌려준다`() = runTest {
        val key = RemoteConfigKey.UseCardFeature
        coEvery { remote.configure(any()) } returns Unit
        coEvery { remote.fetchAndActivate() } throws IOException("network down")

        val repository = repository()
        repository.initialize()

        assertEquals(key.defaultValue, repository.getString(key))
        assertFalse(repository.getBoolean(key))
    }

    @Test
    fun `기능 플래그의 기본값은 꺼짐이다`() {
        assertEquals("false", RemoteConfigKey.UseCardFeature.defaultValue)
    }
}
