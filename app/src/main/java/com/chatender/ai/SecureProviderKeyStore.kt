package com.chatender.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores separate provider credentials encrypted with AES-GCM. */
class SecureProviderKeyStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun save(providerId: String, apiKey: String) {
        val provider = normalize(providerId)
        val cleanKey = apiKey.trim()
        require(cleanKey.isNotEmpty()) { "API key cannot be empty." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(cleanKey.toByteArray(Charsets.UTF_8))
        val value = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(preferences.edit().putString("key_$provider", value).commit()) {
            "Could not save provider API key."
        }
    }

    fun read(providerId: String): String? {
        val stored = preferences.getString("key_${normalize(providerId)}", null) ?: return null
        val parts = stored.split(":", limit = 2)
        require(parts.size == 2) { "Stored API key is invalid. Save it again." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        return String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
    }

    fun hasKey(providerId: String): Boolean = !read(providerId).isNullOrBlank()
    fun clear(providerId: String) { preferences.edit().remove("key_${normalize(providerId)}").apply() }

    private fun normalize(value: String): String {
        val safe = value.trim().lowercase()
        require(safe.matches(Regex("[a-z0-9_-]{2,40}"))) { "Invalid provider ID." }
        return safe
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES = "chatender_secure_provider_keys"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "chatender_multi_provider_master"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
