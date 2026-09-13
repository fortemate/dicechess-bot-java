package com.fortemate.dicechess.bot;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void testResolvePort() {
        assertEquals(8080, Main.resolvePort(null));
        assertEquals(8080, Main.resolvePort(""));
        assertEquals(8080, Main.resolvePort("   "));
        assertEquals(9090, Main.resolvePort("9090"));
        assertEquals(8080, Main.resolvePort("not-a-number"));
    }

    @Test
    void testResolveWebhookKeysWithActiveOnly() {
        var keys = Main.resolveWebhookKeys(Map.of("DICECHESS_WEBHOOK_SECRET", "active-key"));
        assertTrue(keys.isPresent());
        assertEquals("active-key", keys.get().active());
        assertNull(keys.get().pending());
    }

    @Test
    void testResolveWebhookKeysWithPendingOnly() {
        var keys = Main.resolveWebhookKeys(Map.of("DICECHESS_WEBHOOK_NEXT_SECRET", "pending-key"));
        assertTrue(keys.isPresent());
        assertNull(keys.get().active());
        assertEquals("pending-key", keys.get().pending());
    }

    @Test
    void testResolveWebhookKeysWithBothKeys() {
        var keys = Main.resolveWebhookKeys(Map.of(
                "DICECHESS_WEBHOOK_SECRET", "active-key",
                "DICECHESS_WEBHOOK_NEXT_SECRET", "pending-key"
        ));
        assertTrue(keys.isPresent());
        assertEquals("active-key", keys.get().active());
        assertEquals("pending-key", keys.get().pending());
    }

    @Test
    void testResolveWebhookKeysFailsClosedWhenMissingOrBlank() {
        assertTrue(Main.resolveWebhookKeys(Map.of()).isEmpty());
        assertTrue(Main.resolveWebhookKeys(Map.of("DICECHESS_WEBHOOK_SECRET", "   ")).isEmpty());
    }

    @Test
    void testResolveWebhookKeysFromSystemEnvironmentDoesNotThrow() {
        org.junit.jupiter.api.function.ThrowingSupplier<?> supplier = Main::resolveWebhookKeys;
        assertDoesNotThrow(supplier);
    }

    @Test
    void testStartApplicationFailsClosedWithoutKeys() {
        var server = Main.startApplication(Map.of());
        assertNull(server, "startApplication must return null when webhook keys are absent");
    }

    @Test
    void testStartApplicationSuccessWithValidKeys() throws Exception {
        var server = Main.startApplication(Map.of(
                "DICECHESS_WEBHOOK_SECRET", "test-secret",
                "PORT", "0"
        ));
        assertNotNull(server, "startApplication must return running server when keys are provided");
        try {
            var port = server.getAddress().getPort();
            try (var client = HttpClient.newHttpClient()) {
                var req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/health")).GET().build();
                var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, resp.statusCode());
                assertEquals("OK", resp.body());
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void testStartApplicationFailsGracefullyOnInvalidPort() {
        var server = Main.startApplication(Map.of(
                "DICECHESS_WEBHOOK_SECRET", "test-secret",
                "PORT", "-1"
        ));
        assertNull(server, "startApplication should return null on port bind failure");
    }

    @Test
    void testMainMethodExecutesCleanlyWhenUnconfigured() {
        assertDoesNotThrow(() -> Main.main(new String[0]));
    }
}
