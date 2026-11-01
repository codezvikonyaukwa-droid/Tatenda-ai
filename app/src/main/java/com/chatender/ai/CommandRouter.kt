package com.chatender.ai

import java.text.DateFormat
import java.util.Date

enum class CommandAction { HELP, BATTERY, TIME, WIFI_SETTINGS, BLUETOOTH_SETTINGS, DEVICE_SETTINGS, UNKNOWN }
data class CommandResult(val action: CommandAction, val reply: String)

class CommandRouter {
    fun route(raw: String, batteryPercent: Int? = null, now: Date = Date()): CommandResult {
        val input = raw.trim().lowercase()
        if (input.isEmpty()) return CommandResult(CommandAction.UNKNOWN, "I didn't hear a command.")
        return when {
            input in setOf("help", "what can you do", "commands") ->
                CommandResult(CommandAction.HELP, "Ask for battery status, the time, Wi-Fi settings, Bluetooth settings, or device settings. Say what was my last command to recall it, or forget my last command to clear it.")
            "battery" in input || "charge" in input ->
                CommandResult(CommandAction.BATTERY, batteryPercent?.let { "Battery is at $it percent." } ?: "I can't read the battery level right now.")
            input == "time" || "what time" in input ->
                CommandResult(CommandAction.TIME, "The time is " + DateFormat.getTimeInstance(DateFormat.SHORT).format(now) + ".")
            "wifi settings" in input || "wi-fi settings" in input || "open wifi" in input ->
                CommandResult(CommandAction.WIFI_SETTINGS, "Opening Wi-Fi settings.")
            "bluetooth settings" in input || "open bluetooth" in input ->
                CommandResult(CommandAction.BLUETOOTH_SETTINGS, "Opening Bluetooth settings.")
            input == "settings" || "device settings" in input || "open settings" in input ->
                CommandResult(CommandAction.DEVICE_SETTINGS, "Opening device settings.")
            else -> CommandResult(CommandAction.UNKNOWN, "I can help with battery status, the time, Wi-Fi, Bluetooth, and device settings. Say help for commands.")
        }
    }
}
