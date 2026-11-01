package com.chatender.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private var speech: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private val router = CommandRouter()
    private val requestAudio = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        root.addView(title, match())
        root.addView(status, match())
        root.addView(transcript, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(speak, match())
        root.addView(help, match())
        root.addView(battery, match())
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
        val battery = getSystemService(BATTERY_SERVICE) as BatteryManager
        val percent = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 }
        val result = router.route(words, percent)
        respond(result.reply)
        if (result.action == CommandAction.WIFI_SETTINGS) {
            startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
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

    override fun onDestroy() {
        speech?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
