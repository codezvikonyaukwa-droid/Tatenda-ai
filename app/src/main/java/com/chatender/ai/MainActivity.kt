package com.chatender.ai

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.text.InputType
import android.widget.Toast
import java.util.concurrent.Executors
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private var speech: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private val router = CommandRouter()
    private lateinit var memory: ConversationMemory
    private lateinit var history: ConversationHistory
    private lateinit var apiKeyStore: SecureApiKeyStore
    private val aiExecutor = Executors.newSingleThreadExecutor()
    private lateinit var messageInput: EditText
    private val providerPreferences by lazy { getSharedPreferences("chatender_provider_settings", MODE_PRIVATE) }
    private val requestAudio = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        memory = ConversationMemory(this)
        history = ConversationHistory(this)
        apiKeyStore = SecureApiKeyStore(this)
        tts = TextToSpeech(this, this)
        buildInterface()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
            setBackgroundColor(0xFF080D18.toInt())
        }
        val title = TextView(this).apply {
            text = "CHATENDER AI"
            textSize = 27f
            setTextColor(0xFFFFC857.toInt())
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "SYSTEM READY"
            textSize = 13f
            setTextColor(0xFF55D6BE.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
        }
        transcript = TextView(this).apply {
            text = "Tap Speak and give a command."
            textSize = 17f
            setTextColor(0xFFF1F5F9.toInt())
            gravity = Gravity.CENTER
            setPadding(8, 20, 8, 30)
        }
        val speak = Button(this).apply { text = "SPEAK"; setOnClickListener { beginListening() } }
        val help = Button(this).apply { text = "HELP"; setOnClickListener { respond(router.route("help").reply) } }
        val battery = Button(this).apply { text = "BATTERY"; setOnClickListener { handleCommand("battery") } }
        val provider = Button(this).apply {
            text = "AI PROVIDER SETTINGS"
            setOnClickListener { showProviderSettings() }
        }
        messageInput = EditText(this).apply {
            hint = "Message CHATENDER AI"
            setTextColor(0xFFF1F5F9.toInt())
            setHintTextColor(0xFF9AA4B2.toInt())
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 3
        }
        val send = Button(this).apply {
            text = "SEND TO AI"
            setOnClickListener { sendToAi(messageInput.text.toString()) }
        }
        val showHistory = Button(this).apply {
            text = "CHAT HISTORY"
            setOnClickListener { showConversationHistory() }
        }
        val clearHistory = Button(this).apply {
            text = "CLEAR CHAT HISTORY"
            setOnClickListener {
                history.clear()
                respond("Conversation history cleared.")
            }
        }
        val clearMemory = Button(this).apply {
            text = "CLEAR MEMORY"
            setOnClickListener {
                memory.forgetLastCommand()
                respond("The saved last command has been cleared.")
            }
        }
        root.addView(title, match())
        root.addView(status, match())
        root.addView(transcript, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(provider, match())
        root.addView(messageInput, match())
        root.addView(send, match())
        root.addView(speak, match())
        root.addView(help, match())
        root.addView(battery, match())
        root.addView(showHistory, match())
        root.addView(clearHistory, match())
        root.addView(clearMemory, match())
        setContentView(root)
    }

    private fun match() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 10 }

    private fun beginListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            respond("Speech recognition is not available on this device.")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestAudio)
            return
        }
        try {
            speech?.destroy()
            speech = SpeechRecognizer.createSpeechRecognizer(this).also { recognizer ->
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { status.text = "LISTENING…" }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { status.text = "PROCESSING…" }
                    override fun onError(error: Int) {
                        status.text = "READY"
                        transcript.text = "Speech error ($error). Tap Speak to retry."
                    }
                    override fun onResults(results: Bundle?) {
                        val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        transcript.text = "You: $words"
                        handleCommand(words)
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Chatender")
                }
                recognizer.startListening(intent)
            }
        } catch (_: Exception) {
            status.text = "READY"
            respond("I couldn't start speech recognition.")
        }
    }

    private fun handleCommand(words: String) {
        val command = words.trim()
        when {
            command.equals("what was my last command", ignoreCase = true) -> {
                respond(memory.lastCommand()?.let { "Your last command was: $it" } ?: "I don't have a saved command yet.")
                return
            }
            command.equals("forget my last command", ignoreCase = true) || command.equals("clear memory", ignoreCase = true) -> {
                memory.forgetLastCommand()
                respond("I've cleared the saved last command.")
                return
            }
        }
        if (command.isNotEmpty()) memory.rememberCommand(command)
        val battery = getSystemService(BATTERY_SERVICE) as BatteryManager
        val percent = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 }
        val result = router.route(words, percent)
        if (result.action == CommandAction.UNKNOWN) {
            sendToAi(command)
            return
        }
        respond(result.reply)
        val settingsAction = when (result.action) {
            CommandAction.WIFI_SETTINGS -> Settings.ACTION_WIFI_SETTINGS
            CommandAction.BLUETOOTH_SETTINGS -> Settings.ACTION_BLUETOOTH_SETTINGS
            CommandAction.DEVICE_SETTINGS -> Settings.ACTION_SETTINGS
            else -> null
        }
        settingsAction?.let { action ->
            runCatching { startActivity(Intent(action)) }
                .onFailure { respond("I couldn't open that settings screen on this device.") }
        }
    }

    private fun showProviderSettings() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 12, 36, 0)
        }
        val keyInput = EditText(this).apply {
            hint = "OpenRouter API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(0xFF111827.toInt())
        }
        val modelInput = EditText(this).apply {
            hint = "Model (default: openrouter/free)"
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(0xFF111827.toInt())
            setText(providerPreferences.getString("model", "openrouter/free"))
        }
        panel.addView(keyInput, match())
        panel.addView(modelInput, match())
        AlertDialog.Builder(this)
            .setTitle("OpenRouter connection")
            .setMessage("Your key is encrypted with Android Keystore and is not included in the app build.")
            .setView(panel)
            .setPositiveButton("SAVE") { _, _ ->
                val key = keyInput.text.toString().trim()
                val model = modelInput.text.toString().trim().ifBlank { "openrouter/free" }
                runCatching {
                    if (key.isNotEmpty()) apiKeyStore.save(key)
                    providerPreferences.edit().putString("model", model).apply()
                }.onSuccess {
                    respond(if (key.isEmpty()) "Model settings saved. Existing API key was kept." else "OpenRouter key saved securely.")
                }.onFailure { respond("Could not save provider settings: ${it.message ?: "unknown error"}") }
            }
            .setNegativeButton("CANCEL", null)
            .setNeutralButton("REMOVE KEY") { _, _ ->
                apiKeyStore.clear()
                respond("The saved OpenRouter key has been removed.")
            }
            .show()
    }

    private fun showConversationHistory() {
        val saved = history.messages()
        val content = if (saved.isEmpty()) "No saved conversations yet." else saved.joinToString("\n\n") {
            "${if (it.role == "user") "You" else "CHATENDER"}: ${it.content}"
        }
        AlertDialog.Builder(this)
            .setTitle("Recent chat history")
            .setMessage(content)
            .setPositiveButton("CLOSE", null)
            .show()
    }

    private fun sendToAi(rawMessage: String) {
        val message = rawMessage.trim()
        if (message.isEmpty()) {
            respond("Type or speak a message first.")
            return
        }
        val key = runCatching { apiKeyStore.read() }.getOrElse {
            respond("The saved API key could not be read. Remove it and save it again.")
            return
        }
        if (key.isNullOrBlank()) {
            respond("Connect OpenRouter first. Tap AI Provider Settings and add your API key.")
            return
        }
        val model = providerPreferences.getString("model", "openrouter/free") ?: "openrouter/free"
        messageInput.setText("")
        history.add("user", message)
        val contextMessages = history.messages()
        transcript.text = "You: $message\n\nCHATENDER: Thinking…"
        status.text = "CONNECTING TO AI…"
        aiExecutor.execute {
            val answer = runCatching { OpenRouterClient(key, model).ask(contextMessages) }
                .getOrElse { "AI request failed: ${it.message ?: "Check your connection and provider settings."}" }
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    history.add("assistant", answer)
                    respond(answer)
                }
            }
        }
    }

    private fun respond(message: String) {
        transcript.text = message
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "chatender-reply")
        status.text = "READY"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestAudio && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) beginListening()
        else if (requestCode == requestAudio) respond("Microphone permission was not granted.")
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
    }

    override fun onPause() {
        speech?.cancel()
        super.onPause()
    }

    override fun onDestroy() {
        speech?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
