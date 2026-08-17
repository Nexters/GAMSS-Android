package com.gamss.android.data.push

import com.gamss.android.domain.push.PushTokenProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
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
            firebaseMessagingProvider.get().token.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
