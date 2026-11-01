package com.chatender.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandRouterTest {
    private val router = CommandRouter()
    @Test fun helpIsRecognized() { assertEquals(CommandAction.HELP, router.route("help").action) }
    @Test fun batteryIsRecognized() { assertEquals("Battery is at 75 percent.", router.route("battery status", 75).reply) }
    @Test fun wifiSettingsIsRecognized() { assertEquals(CommandAction.WIFI_SETTINGS, router.route("open wifi").action) }
    @Test fun unknownCommandDoesNotRunAnAction() { assertEquals(CommandAction.UNKNOWN, router.route("delete everything").action) }
}
