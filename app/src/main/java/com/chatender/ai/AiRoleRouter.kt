package com.chatender.ai

enum class AiRole(val id: String, val label: String, val instruction: String) {
    CODING("coding", "Coding", "Act as a careful senior software engineer. Explain assumptions, produce tested code, and prioritize security."),
    MEMORY("memory", "Memory", "Organize and retrieve only user-approved saved information. Be clear about uncertainty and never invent memories."),
    DIAGNOSIS("diagnosis", "Diagnostics", "Diagnose device or software problems step by step. Warn before destructive actions."),
    GENERAL("general", "General", "Answer accurately and concisely. State uncertainty when information is missing.")
}

data class AiRoute(val role: AiRole, val providerId: String, val modelId: String)

class AiRoleRouter(private val settings: Map<String, String>) {
    fun route(message: String): AiRoute {
        val text = message.lowercase()
        val role = when {
            listOf("code", "coding", "program", "script", "bug", "compile", "apk", "html", "kotlin", "python").any(text::contains) -> AiRole.CODING
            listOf("remember", "memory", "what did i say", "recall", "save this").any(text::contains) -> AiRole.MEMORY
            listOf("diagnose", "diagnosis", "error", "crash", "slow", "battery", "wifi", "wi-fi", "network", "not working", "troubleshoot").any(text::contains) -> AiRole.DIAGNOSIS
            else -> AiRole.GENERAL
        }
        val provider = settings["${role.id}_provider"]?.takeIf { it.isNotBlank() } ?: "openrouter"
        val model = settings["${role.id}_model"]?.takeIf { it.isNotBlank() } ?: "openrouter/free"
        return AiRoute(role, provider, model)
    }

    companion object {
        fun defaultVoiceId(): String = "e13fa398a7f445a685316a3de6089ce7"
    }
}
