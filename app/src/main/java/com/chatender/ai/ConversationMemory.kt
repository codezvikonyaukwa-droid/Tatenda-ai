package com.chatender.ai

import android.content.Context

/** Stores only the most recent voice command in this app's private preferences. */
class ConversationMemory(context: Context) {
    private val preferences = context.getSharedPreferences("chatender_private_memory", Context.MODE_PRIVATE)

    fun rememberCommand(command: String) {
        val safeCommand = command.trim().take(160)
        if (safeCommand.isNotEmpty()) preferences.edit().putString(KEY_LAST_COMMAND, safeCommand).apply()
    }

    fun lastCommand(): String? = preferences.getString(KEY_LAST_COMMAND, null)

    fun forgetLastCommand() {
        preferences.edit().remove(KEY_LAST_COMMAND).apply()
    }

    private companion object {
        const val KEY_LAST_COMMAND = "last_command"
    }
}
