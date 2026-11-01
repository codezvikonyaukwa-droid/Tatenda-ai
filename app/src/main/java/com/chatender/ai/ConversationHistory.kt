package com.chatender.ai

import android.content.Context
import android.util.Base64

data class ChatMessage(val role: String, val content: String)

/** Keeps a bounded conversation transcript in app-private storage. */
class ConversationHistory internal constructor(private val storage: MemoryStorage) {
    constructor(context: Context) : this(
        PreferencesMemoryStorageForHistory(context.getSharedPreferences("chatender_chat_history", Context.MODE_PRIVATE))
    )

    fun messages(): List<ChatMessage> {
        val encoded = storage.read(KEY_MESSAGES).orEmpty()
        return encoded.lineSequence().filter { it.isNotBlank() }.mapNotNull { line ->
            runCatching {
                val decoded = String(Base64.decode(line, Base64.DEFAULT), Charsets.UTF_8)
                val split = decoded.indexOf('\n')
                if (split <= 0) null else ChatMessage(decoded.substring(0, split), decoded.substring(split + 1))
            }.getOrNull()
        }.toList().takeLast(MAX_MESSAGES)
    }

    fun add(role: String, content: String) {
        if (role !in setOf("user", "assistant") || content.isBlank()) return
        val next = (messages() + ChatMessage(role, content.take(MAX_CONTENT_LENGTH))).takeLast(MAX_MESSAGES)
        val encoded = next.joinToString("\n") {
            Base64.encodeToString("${it.role}\n${it.content}".toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
        storage.write(KEY_MESSAGES, encoded)
    }

    fun clear() = storage.remove(KEY_MESSAGES)

    private companion object {
        const val KEY_MESSAGES = "messages"
        const val MAX_MESSAGES = 20
        const val MAX_CONTENT_LENGTH = 2_000
    }
}

private class PreferencesMemoryStorageForHistory(
    private val preferences: android.content.SharedPreferences
) : MemoryStorage {
    override fun read(key: String): String? = preferences.getString(key, null)
    override fun write(key: String, value: String) { preferences.edit().putString(key, value).apply() }
    override fun remove(key: String) { preferences.edit().remove(key).apply() }
}
