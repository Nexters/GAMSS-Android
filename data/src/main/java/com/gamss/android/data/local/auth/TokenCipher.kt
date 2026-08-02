package com.gamss.android.data.local.auth

import android.util.Base64
import com.google.crypto.tink.Aead
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal interface TokenCipher {
    fun encrypt(value: String, associatedData: String): String

    fun decrypt(value: String, associatedData: String): String
}

@Singleton
internal class TinkTokenCipher @Inject constructor(
    @param:AuthTokenAead private val aeadProvider: Provider<Aead>,
) : TokenCipher {

    override fun encrypt(value: String, associatedData: String): String {
        val ciphertext = aeadProvider.get().encrypt(
            value.toByteArray(StandardCharsets.UTF_8),
            associatedData.toByteArray(StandardCharsets.UTF_8),
        )
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    override fun decrypt(value: String, associatedData: String): String {
        val plaintext = aeadProvider.get().decrypt(
            Base64.decode(value, Base64.NO_WRAP),
            associatedData.toByteArray(StandardCharsets.UTF_8),
        )
        return String(plaintext, StandardCharsets.UTF_8)
    }
}
