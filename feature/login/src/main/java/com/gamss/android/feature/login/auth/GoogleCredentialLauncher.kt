package com.gamss.android.feature.login.auth

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class GoogleCredentialLauncher(
    private val context: Context,
    private val credentialManager: CredentialManager,
    private val credentialRequest: GetCredentialRequest,
    private val coroutineScope: CoroutineScope,
) {
    fun launch(
        onSuccess: (idToken: String) -> Unit,
        onCancel: () -> Unit,
        onFailure: () -> Unit,
    ) {
        coroutineScope.launch {
            try {
                val credential = credentialManager.getCredential(
                    context = context,
                    request = credentialRequest,
                ).credential

                if (
                    credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    onSuccess(googleCredential.idToken)
                } else {
                    onFailure()
                }
            } catch (e: GetCredentialCancellationException) {
                onCancel()
            } catch (e: GetCredentialException) {
                Log.w(TAG, "getCredential failed", e)
                onFailure()
            } catch (e: GoogleIdTokenParsingException) {
                Log.w(TAG, "Google ID token 파싱 실패", e)
                onFailure()
            }
        }
    }

    private companion object {
        const val TAG = "GoogleCredentialLauncher"
    }
}

@Composable
internal fun rememberGoogleCredentialLauncher(
    googleWebClientId: String,
): GoogleCredentialLauncher {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val credentialRequest = remember(googleWebClientId) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(googleWebClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()

        GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    return remember(context, credentialManager, credentialRequest, coroutineScope) {
        GoogleCredentialLauncher(
            context = context,
            credentialManager = credentialManager,
            credentialRequest = credentialRequest,
            coroutineScope = coroutineScope,
        )
    }
}
