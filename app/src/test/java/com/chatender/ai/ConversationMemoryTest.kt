package com.chatender.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationMemoryTest {
    private class FakeStorage : MemoryStorage {
        private val values = mutableMapOf<String, String>()
        override fun read(key: String): String? = values[key]
        override fun write(key: String, value: String) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }

    @Test fun remembersTrimmedCommand() {
        val memory = ConversationMemory(FakeStorage())
        memory.rememberCommand("  check battery  ")
        assertEquals("check battery", memory.lastCommand())
    }

    @Test fun ignoresEmptyCommand() {
        val memory = ConversationMemory(FakeStorage())
        memory.rememberCommand("   ")
        assertNull(memory.lastCommand())
    }

    @Test fun limitsStoredCommandLength() {
        val memory = ConversationMemory(FakeStorage())
        memory.rememberCommand("x".repeat(200))
        assertEquals(160, memory.lastCommand()?.length)
    }

    @Test fun forgetsLastCommand() {
        val memory = ConversationMemory(FakeStorage())
        memory.rememberCommand("open settings")
        memory.forgetLastCommand()
        assertNull(memory.lastCommand())
    }
}
