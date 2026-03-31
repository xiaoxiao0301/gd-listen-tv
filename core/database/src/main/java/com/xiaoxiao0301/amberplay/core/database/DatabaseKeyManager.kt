package com.xiaoxiao0301.amberplay.core.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Generates and persists the SQLCipher passphrase using Android Keystore.
 *
 * Strategy:
 *  - An AES-256-GCM key is generated (once) in the Android Keystore under the
 *    alias [KEYSTORE_ALIAS].  The raw key material never leaves the Keystore.
 *  - On first launch a random 32-byte passphrase is encrypted with that key
 *    (AES/GCM/NoPadding) and the resulting ciphertext + IV are Base64-stored in
 *    a private SharedPreferences file.
 *  - On every subsequent launch the ciphertext is decrypted to recover the same
 *    passphrase, which is then passed to SQLCipher's SupportFactory.
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

    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encPassB64 = prefs.getString(KEY_ENCRYPTED_PASS, null)
        val ivB64      = prefs.getString(KEY_IV, null)
        val keystoreKey = getOrCreateKeystoreKey()

        if (encPassB64 != null && ivB64 != null) {
            val encPass = Base64.decode(encPassB64, Base64.DEFAULT)
            val iv      = Base64.decode(ivB64, Base64.DEFAULT)
            val cipher  = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            return cipher.doFinal(encPass)
        }

        // First launch: generate a random passphrase, encrypt and persist it.
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
