package com.fortemate.dicechess.bot;

import com.fortemate.dicechess.runtime.CustomHandlerServer;
import com.fortemate.dicechess.runtime.Signatures;
import com.fortemate.dicechess.runtime.WebhookHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookIntegrationTest {

    private static final String SECRET = "test-secret";

    private HttpServer server;
    private OnnxEvaluator evaluator;

    @BeforeEach
    void setUp() throws IOException {
        evaluator = new OnnxEvaluator(null);
        var strategy = new OnnxStrategy(evaluator);
        var handler = new WebhookHandler(SECRET, strategy);

        // Bind on ephemeral port 0
        server = CustomHandlerServer.start(0, "/api/webhook", handler);
        server.createContext("/health", exchange -> {
            var response = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (var os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.createContext("/", exchange -> {
            var response = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (var os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        if (evaluator != null) {
            evaluator.close();
        }
    }

    @Test
    void testWebhookRejectsUnauthenticatedRequest() throws Exception {
        var port = server.getAddress().getPort();
        var client = HttpClient.newHttpClient();

        // A bare GET without proper HMAC signature headers should be rejected
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/webhook"))
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode(), "Unauthenticated request should return 400");
    }

    @Test
    void testVerificationHandshake() throws Exception {
        var port = server.getAddress().getPort();
        var client = HttpClient.newHttpClient();

        var body = "{\"type\":\"verification\",\"nonce\":\"test-nonce-123\"}";
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/webhook"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Handshake should succeed with 200");
        assertTrue(response.body().contains("test-nonce-123"), "Response should echo the nonce");
    }

    @Test
    void testSignedYourTurnDelivery() throws Exception {
        var port = server.getAddress().getPort();
        var client = HttpClient.newHttpClient();

        var body = """
                {"type":"yourTurn","gameId":"game-123","seat":"White","state":{"version":1,"dfen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 p","activeSeat":"White","dicePending":true}}
                """.strip();
        var now = Instant.now().getEpochSecond();
        var signature = Signatures.sign(SECRET, now, body);

        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/webhook"))
                .header("Content-Type", "application/json")
                .header(WebhookHandler.TIMESTAMP_HEADER, String.valueOf(now))
                .header(WebhookHandler.SIGNATURE_HEADER, signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Signed delivery should succeed with 200");
        assertTrue(response.body().contains("\"moves\":["), "Response should contain moves");
        assertTrue(response.body().contains("\"offerDraw\":false"), "Response should specify offerDraw");
    }

    @Test
    void testSignedDeliveryWithInvalidSignature() throws Exception {
        var port = server.getAddress().getPort();
        var client = HttpClient.newHttpClient();

        var body = """
                {"type":"yourTurn","gameId":"game-123","seat":"White","state":{"version":1,"dfen":"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 p","activeSeat":"White","dicePending":true}}
                """.strip();
        var now = Instant.now().getEpochSecond();

        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/webhook"))
                .header("Content-Type", "application/json")
                .header(WebhookHandler.TIMESTAMP_HEADER, String.valueOf(now))
                .header(WebhookHandler.SIGNATURE_HEADER, "invalid-signature")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode(), "Bad signature should return 401");
    }

    @Test
    void testHealthCheckAndRootEndpoints() throws Exception {
        var port = server.getAddress().getPort();
        var client = HttpClient.newHttpClient();

        var healthReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/health"))
                .GET()
                .build();
        var healthResp = client.send(healthReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, healthResp.statusCode());
        assertEquals("OK", healthResp.body());

        var rootReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/"))
                .GET()
                .build();
        var rootResp = client.send(rootReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, rootResp.statusCode());
        assertEquals("OK", rootResp.body());
    }

    @Test
    void testMainResolveWebhookKeys() {
        var configured = Main.resolveWebhookKeys(Map.of("DICECHESS_WEBHOOK_SECRET", "custom-secret"));
        assertTrue(configured.hasActive());
        assertEquals("custom-secret", configured.active());

        var unconfigured = Main.resolveWebhookKeys(Map.of());
        assertTrue(unconfigured.hasActive());
        assertEquals("unconfigured-secret", unconfigured.active());
    }
}
