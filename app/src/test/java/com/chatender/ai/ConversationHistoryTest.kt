package com.chatender.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationHistoryTest {
    private class FakeStorage : MemoryStorage {
        private val values = mutableMapOf<String, String>()
        override fun read(key: String): String? = values[key]
        override fun write(key: String, value: String) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }

    @Test fun savesAndRestoresMessages() {
        val history = ConversationHistory(FakeStorage())
        history.add("user", "Hello")
        history.add("assistant", "Hi there")
        assertEquals(listOf(ChatMessage("user", "Hello"), ChatMessage("assistant", "Hi there")), history.messages())
    }

    @Test fun boundsHistoryToTwentyMessages() {
        val history = ConversationHistory(FakeStorage())
        repeat(25) { history.add("user", "message $it") }
        assertEquals(20, history.messages().size)
        assertEquals("message 5", history.messages().first().content)
    }

    @Test fun clearRemovesAllMessages() {
        val history = ConversationHistory(FakeStorage())
        history.add("user", "erase me")
        history.clear()
        assertTrue(history.messages().isEmpty())
    }
}
