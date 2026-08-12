package com.gamss.android.feature.login.auth

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [GoogleCredentialLauncher.launch]가 콜백 대신 suspend 함수로 결과를 반환하도록 리팩토링되며
 * 각 [CredentialManager] 응답/예외가 올바른 [GoogleCredentialResult]로 매핑되는지 검증한다.
 */
class GoogleCredentialLauncherTest {

    private val context: Context = mockk(relaxed = true)
    private val credentialManager: CredentialManager = mockk()
    private val credentialRequest: GetCredentialRequest = mockk()

    private fun launcher() = GoogleCredentialLauncher(
        context = context,
        credentialManager = credentialManager,
        credentialRequest = credentialRequest,
    )

    @Before
    fun setUp() {
        // GetCredentialException catch 분기의 Log.w 호출이 unit test 환경(android.jar 스텁)에서
        // "not mocked" RuntimeException을 던지지 않도록 스텁 처리한다.
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkObject(GoogleIdTokenCredential.Companion)
        unmockkStatic(Log::class)
    }

    @Test
    fun `Google ID 토큰 크리덴셜을 성공적으로 받으면 Success를 반환한다`() = runTest {
        val expectedIdToken = "fake-google-id-token"
        val credential = CustomCredential(
            type = GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            data = mockk<Bundle>(),
        )
        coEvery {
            credentialManager.getCredential(context = context, request = credentialRequest)
        } returns GetCredentialResponse(credential)

        mockkObject(GoogleIdTokenCredential.Companion)
        every { GoogleIdTokenCredential.createFrom(any()) } returns mockk {
            every { idToken } returns expectedIdToken
        }

        val result = launcher().launch()

        assertTrue(result is GoogleCredentialResult.Success)
        assertEquals(expectedIdToken, (result as GoogleCredentialResult.Success).idToken)
    }

    @Test
    fun `사용자가 크리덴셜 선택을 취소하면 Cancelled를 반환한다`() = runTest {
        coEvery {
            credentialManager.getCredential(context = context, request = credentialRequest)
        } throws GetCredentialCancellationException()

        val result = launcher().launch()

        assertEquals(GoogleCredentialResult.Cancelled, result)
    }

    @Test
    fun `CredentialManager 요청이 실패하면 Failure를 반환한다`() = runTest {
        coEvery {
            credentialManager.getCredential(context = context, request = credentialRequest)
        } throws GetCredentialUnknownException()

        val result = launcher().launch()

        assertEquals(GoogleCredentialResult.Failure, result)
    }

    @Test
    fun `Google ID 토큰이 아닌 크리덴셜을 받으면 Failure를 반환한다`() = runTest {
        val credential = CustomCredential(
            type = "com.example.unsupported.CREDENTIAL",
            data = mockk<Bundle>(),
        )
        coEvery {
            credentialManager.getCredential(context = context, request = credentialRequest)
        } returns GetCredentialResponse(credential)

        val result = launcher().launch()

        assertEquals(GoogleCredentialResult.Failure, result)
    }
}
