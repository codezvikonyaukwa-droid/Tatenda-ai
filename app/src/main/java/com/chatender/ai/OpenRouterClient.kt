package com.chatender.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Small synchronous client. Call from a worker thread, never the Android main thread. */
class OpenRouterClient(
    private val apiKey: String,
    private val model: String = "openrouter/free"
) {
    fun ask(userMessage: String): String {
        require(apiKey.isNotBlank()) { "Add your OpenRouter API key in Settings first." }
        require(userMessage.isNotBlank()) { "Message cannot be empty." }
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("HTTP-Referer", "https://github.com/codezvikonyaukwa-droid/Tatenda-ai")
            setRequestProperty("X-Title", "CHATENDER AI")
        }
        try {
            val body = JSONObject()
                .put("model", model)
                .put("messages", JSONArray().put(
                    JSONObject().put("role", "user").put("content", userMessage.take(MAX_MESSAGE_LENGTH))
                ))
                .put("temperature", 0.7)
                .put("max_tokens", 700)
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val detail = runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull()
                throw IllegalStateException("OpenRouter error ($status): ${detail?.takeIf { it.isNotBlank() } ?: "Request failed."}")
            }
            val content = JSONObject(response)
                .optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.optString("content")
                ?.takeIf { it.isNotBlank() && it != "null" }
            return content ?: "The AI provider returned an empty response."
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
        const val MAX_MESSAGE_LENGTH = 4_000
    }
}
