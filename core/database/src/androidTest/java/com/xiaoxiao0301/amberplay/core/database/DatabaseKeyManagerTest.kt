package com.xiaoxiao0301.amberplay.core.database

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/**
 * Instrumentation tests for [DatabaseKeyManager].
 *
 * Each test starts from a clean slate: the amber_db_meta SharedPreferences and
 * the Keystore alias are deleted in [setUp] and [tearDown] so tests are independent.
 *
 * The factory-reset recovery path is exercised by writing a deliberately
 * corrupted Base64 ciphertext to SharedPreferences before calling
 * [DatabaseKeyManager.getOrCreatePassphrase].  With valid stored entries present
 * but an unreadable ciphertext, the Cipher.doFinal() call throws
 * BadPaddingException (a GeneralSecurityException subclass), triggering the same
 * code path that would fire after a real factory reset.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseKeyManagerTest {

    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val prefs get() = ctx.getSharedPreferences("amber_db_meta", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        clearState()
    }

    @After
    fun tearDown() {
        clearState()
    }

    private fun clearState() {
        prefs.edit().clear().apply()
        runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            if (ks.containsAlias("amber_db_master_key")) {
                ks.deleteEntry("amber_db_master_key")
            }
        }
    }

    // ── happy-path ────────────────────────────────────────────────────────────

    @Test
    fun firstCall_returnsNonEmptyPassphrase() {
        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(ctx)

        assertNotNull(passphrase)
        assertEquals(32, passphrase.size)
        // Must not be all zeros (would indicate SecureRandom failure)
        assertTrue(passphrase.any { it != 0.toByte() })
        passphrase.fill(0)
    }

    @Test
    fun secondCall_returnsIdenticalPassphrase() {
        val first  = DatabaseKeyManager.getOrCreatePassphrase(ctx)
        val second = DatabaseKeyManager.getOrCreatePassphrase(ctx)

        assertArrayEquals("Passphrase must survive across calls", first, second)
        first.fill(0)
        second.fill(0)
    }

    @Test
    fun normalOperation_doesNotSetRecoveryFlag() {
        DatabaseKeyManager.getOrCreatePassphrase(ctx).fill(0)

        assertFalse(DatabaseKeyManager.lastWasRecoveredFromKeystoreFailure)
    }

    // ── factory-reset / Keystore-eviction recovery ────────────────────────────

    @Test
    fun corruptedCiphertext_triggersRecovery_andSetsFlag() {
        // Simulate a pre-existing record: valid-looking Base64 strings that
        // cannot be decrypted by the current Keystore key (mimics factory reset
        // where the old key is gone and a new key was auto-generated).
        val garbage = Base64.encodeToString(ByteArray(48) { it.toByte() }, Base64.DEFAULT)
        val dummyIv = Base64.encodeToString(ByteArray(12) { 0x00 }, Base64.DEFAULT)
        prefs.edit()
            .putString("ep", garbage)
            .putString("iv", dummyIv)
            .apply()

        val passphrase = DatabaseKeyManager.getOrCreatePassphrase(ctx)

        // Recovery flag must be raised
        assertTrue(
            "lastWasRecoveredFromKeystoreFailure should be true after decryption failure",
            DatabaseKeyManager.lastWasRecoveredFromKeystoreFailure,
        )

        // Stale prefs must have been cleared (new entries written by fresh-gen path)
        val storedCiphertext = prefs.getString("ep", null)
        assertNotNull("A new encrypted passphrase should have been stored", storedCiphertext)
        // The new ciphertext must differ from the garbage we injected
        assertTrue(storedCiphertext != garbage)

        // A valid 32-byte passphrase must still be returned
        assertEquals(32, passphrase.size)
        passphrase.fill(0)
    }

    @Test
    fun afterRecovery_secondCall_returnsSameFreshPassphrase() {
        // Inject corrupted ciphertext → triggers recovery on first call
        val garbage = Base64.encodeToString(ByteArray(48) { 0x42 }, Base64.DEFAULT)
        val dummyIv = Base64.encodeToString(ByteArray(12) { 0x00 }, Base64.DEFAULT)
        prefs.edit().putString("ep", garbage).putString("iv", dummyIv).apply()

        val afterRecovery  = DatabaseKeyManager.getOrCreatePassphrase(ctx)
        // Second call should now succeed normally (new key present in prefs)
        val afterRecovery2 = DatabaseKeyManager.getOrCreatePassphrase(ctx)

        assertArrayEquals(
            "Two calls after recovery should return the same new passphrase",
            afterRecovery,
            afterRecovery2,
        )
        assertFalse(
            "Recovery flag must be false on the second (normal) call",
            DatabaseKeyManager.lastWasRecoveredFromKeystoreFailure,
        )

        afterRecovery.fill(0)
        afterRecovery2.fill(0)
    }
}
