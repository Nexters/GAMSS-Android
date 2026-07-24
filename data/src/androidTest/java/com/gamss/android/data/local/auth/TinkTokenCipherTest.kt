package com.gamss.android.data.local.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.crypto.tink.integration.android.AndroidKeystore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.GeneralSecurityException
import java.util.UUID
import javax.inject.Provider

@RunWith(AndroidJUnit4::class)
class TinkTokenCipherTest {

    private lateinit var keyAlias: String
    private lateinit var tokenCipher: TinkTokenCipher

    @Before
    fun setUp() {
        keyAlias = "gamss_auth_token_test_${UUID.randomUUID()}"
        AndroidKeystore.generateNewAes256GcmKey(keyAlias)
        tokenCipher = TinkTokenCipher(
            Provider { AndroidKeystore.getAead(keyAlias) },
        )
    }

    @After
    fun tearDown() {
        AndroidKeystore.deleteKey(keyAlias)
    }

    @Test
    fun encryptAndDecrypt() {
        val encrypted = tokenCipher.encrypt(
            value = "access-token",
            associatedData = "access-token-associated-data",
        )

        val decrypted = tokenCipher.decrypt(
            value = encrypted,
            associatedData = "access-token-associated-data",
        )

        assertEquals("access-token", decrypted)
    }

    @Test
    fun decryptWithDifferentAssociatedDataFails() {
        val encrypted = tokenCipher.encrypt(
            value = "access-token",
            associatedData = "access-token-associated-data",
        )

        assertThrows(GeneralSecurityException::class.java) {
            tokenCipher.decrypt(
                value = encrypted,
                associatedData = "refresh-token-associated-data",
            )
        }
    }
}
