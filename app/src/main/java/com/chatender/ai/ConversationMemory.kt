package com.chatender.ai

import android.content.Context

/** Minimal storage contract so memory behavior can be tested without Android services. */
internal interface MemoryStorage {
    fun read(key: String): String?
    fun write(key: String, value: String)
    fun remove(key: String)
}

/** Stores only the most recent voice command in this app's private preferences. */
class ConversationMemory internal constructor(private val storage: MemoryStorage) {
    constructor(context: Context) : this(
        PreferencesMemoryStorage(context.getSharedPreferences("chatender_private_memory", Context.MODE_PRIVATE))
    )

    fun rememberCommand(command: String) {
        val safeCommand = command.trim().take(MAX_COMMAND_LENGTH)
        if (safeCommand.isNotEmpty()) storage.write(KEY_LAST_COMMAND, safeCommand)
    }

    fun lastCommand(): String? = storage.read(KEY_LAST_COMMAND)

    fun forgetLastCommand() {
        storage.remove(KEY_LAST_COMMAND)
    }

    private companion object {
        const val KEY_LAST_COMMAND = "last_command"
        const val MAX_COMMAND_LENGTH = 160
    }
}

private class PreferencesMemoryStorage(
    private val preferences: android.content.SharedPreferences
) : MemoryStorage {
    override fun read(key: String): String? = preferences.getString(key, null)
    override fun write(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}
