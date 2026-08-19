package com.gamss.android.data.push

import com.gamss.android.domain.push.PushTokenProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
internal class FirebaseMessagingTokenProvider @Inject constructor(
    private val firebaseMessagingProvider: Provider<FirebaseMessaging>,
) : PushTokenProvider {

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun getToken(): String? =
        try {
            withTimeoutOrNull(TOKEN_FETCH_TIMEOUT_MILLIS) {
                firebaseMessagingProvider.get().token.await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    private companion object {
        const val TOKEN_FETCH_TIMEOUT_MILLIS = 10_000L
    }
}
