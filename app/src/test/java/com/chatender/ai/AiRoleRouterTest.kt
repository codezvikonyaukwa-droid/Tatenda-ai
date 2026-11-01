package com.chatender.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiRoleRouterTest {
    @Test fun routesCodeTasksToCodingRole() {
        assertEquals(AiRole.CODING, AiRoleRouter(emptyMap()).route("Fix this Kotlin compile error").role)
    }
    @Test fun routesDeviceProblemsToDiagnosticsRole() {
        assertEquals(AiRole.DIAGNOSIS, AiRoleRouter(emptyMap()).route("Diagnose my slow Wi-Fi").role)
    }
    @Test fun routesRecallRequestsToMemoryRole() {
        assertEquals(AiRole.MEMORY, AiRoleRouter(emptyMap()).route("What did I ask you to remember?").role)
    }
    @Test fun usesConfiguredModelPerRole() {
        val route = AiRoleRouter(mapOf("coding_provider" to "openrouter", "coding_model" to "provider/code-model"))
            .route("Write a Python script")
        assertEquals("provider/code-model", route.modelId)
    }
}
