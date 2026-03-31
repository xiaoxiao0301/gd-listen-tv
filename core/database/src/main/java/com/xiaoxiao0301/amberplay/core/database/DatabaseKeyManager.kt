package com.xiaoxiao0301.amberplay.core.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Generates and persists the SQLCipher passphrase using Android Keystore.
 *
 * Normal flow:
 *  1. An AES-256-GCM key is generated once in the Android Keystore (alias
 *     [KEYSTORE_ALIAS]).  Raw key material never leaves the Keystore.
 *  2. On first launch a random 32-byte passphrase is encrypted with that key
 *     (AES/GCM/NoPadding), and the resulting ciphertext + IV are saved
 *     Base64-encoded in a private SharedPreferences file.
 *  3. On every subsequent launch the ciphertext is decrypted to recover the
 *     same passphrase, which is then passed to SQLCipher's SupportFactory.
 *
 * Factory-reset / Keystore-wipe recovery:
 *  Android Keystore keys are destroyed on factory reset (and on some devices
 *  after a failed lock-screen PIN attempt limit). When that happens the stored
 *  ciphertext can no longer be decrypted, so [getOrCreatePassphrase] catches
 *  the resulting [GeneralSecurityException], clears the stale SharedPreferences
 *  entry, and generates a brand-new passphrase.
 *
 *  After recovery [lastWasRecoveredFromKeystoreFailure] is set to `true`.
 *  [DatabaseModule] reads this flag and deletes the now-unreadable database
 *  file before giving Room a fresh start — avoiding a crash at first query.
 *
 *  Consequence for the user: all locally stored music data (playlists, history,
 *  favorites, queue, statistics) is lost after a factory reset.  This is
 *  intentional for a "data stays on device" app and should be stated in the
 *  user-facing documentation.
 *
 * The caller is responsible for zeroing the returned array after use.
 */
internal object DatabaseKeyManager {

    private const val KEYSTORE_PROVIDER  = "AndroidKeyStore"
    private const val KEYSTORE_ALIAS     = "amber_db_master_key"
    private const val PREFS_NAME         = "amber_db_meta"
    private const val KEY_ENCRYPTED_PASS = "ep"
    private const val KEY_IV             = "iv"
    private const val AES_GCM            = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS      = 256
    private const val PASS_BYTES         = 32
    private const val GCM_TAG_BITS       = 128

    /**
     * `true` when the most recent [getOrCreatePassphrase] call had to
     * regenerate the passphrase because the Keystore key was gone (e.g. after
     * a factory reset).  Reset to `false` at the start of each call.
     *
     * [DatabaseModule] uses this flag to delete the old, now-unreadable
     * database file before constructing a new Room instance.
     */
    @Volatile
    var lastWasRecoveredFromKeystoreFailure: Boolean = false
        private set

    fun getOrCreatePassphrase(context: Context): ByteArray {
        lastWasRecoveredFromKeystoreFailure = false

        val prefs       = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encPassB64  = prefs.getString(KEY_ENCRYPTED_PASS, null)
        val ivB64       = prefs.getString(KEY_IV, null)
        val keystoreKey = getOrCreateKeystoreKey()

        if (encPassB64 != null && ivB64 != null) {
            try {
                val encPass = Base64.decode(encPassB64, Base64.DEFAULT)
                val iv      = Base64.decode(ivB64, Base64.DEFAULT)
                val cipher  = Cipher.getInstance(AES_GCM)
                cipher.init(Cipher.DECRYPT_MODE, keystoreKey, GCMParameterSpec(GCM_TAG_BITS, iv))
                return cipher.doFinal(encPass)
            } catch (e: GeneralSecurityException) {
                // The Keystore key was re-created (factory reset / key eviction).
                // The stored ciphertext is permanently unreadable — clear it so a
                // fresh passphrase is generated below.  DatabaseModule will delete
                // the stale database file when it sees lastWasRecoveredFromKeystoreFailure.
                lastWasRecoveredFromKeystoreFailure = true
                prefs.edit().remove(KEY_ENCRYPTED_PASS).remove(KEY_IV).apply()
            }
        }

        // First launch OR post-recovery: generate a new random passphrase,
        // encrypt it with the (new) Keystore key, and persist it.
        val passphrase = ByteArray(PASS_BYTES)
        SecureRandom().nextBytes(passphrase)
        val cipher  = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey)
        val encPass = cipher.doFinal(passphrase)
        val iv      = cipher.iv

        prefs.edit()
            .putString(KEY_ENCRYPTED_PASS, Base64.encodeToString(encPass, Base64.DEFAULT))
            .putString(KEY_IV,             Base64.encodeToString(iv,      Base64.DEFAULT))
            .apply()

        return passphrase
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            return keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
        }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()
        )
        return keyGen.generateKey()
    }
}
