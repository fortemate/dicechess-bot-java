package com.fortemate.dicechess.bot;

import com.fortemate.dicechess.runtime.CustomHandlerServer;
import com.fortemate.dicechess.runtime.WebhookHandler;
import com.fortemate.dicechess.runtime.WebhookKeys;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Entry point for the Dice Chess Java bot starter template.
 */
public class Main {

    private static final Logger logger = System.getLogger(Main.class.getName());

    private static final String DEFAULT_WEBHOOK_PATH = "/api/webhook";

    private Main() {
        // Utility / entry point class
    }

    /**
     * Starts the bot application, loads configuration from environment variables,
     * and binds the embedded HTTP webhook server.
     *
     * @param args command-line arguments (currently unused)
     */
    @SuppressWarnings("java:S1172")
    public static void main(String[] args) {
        var keys = resolveWebhookKeys();
        var modelPath = System.getenv().getOrDefault("MODEL_PATH", "models/baseline.onnx");
        var port = resolvePort();

        var evaluator = new OnnxEvaluator(modelPath);
        var strategy = new OnnxStrategy(evaluator);

        HttpServer server;
        try {
            server = start(port, keys, strategy);
            // Register health check endpoints for Koyeb / Cloud Run / Kubernetes
            server.createContext("/", exchange -> {
                var response = "OK".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                try (var os = exchange.getResponseBody()) {
                    os.write(response);
                }
            });
            server.createContext("/health", exchange -> {
                var response = "OK".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                try (var os = exchange.getResponseBody()) {
                    os.write(response);
                }
            });
        } catch (IOException e) {
            logger.log(Level.ERROR, "Failed to start HTTP server on port {0}: {1}", port, e.getMessage());
            evaluator.close();
            return;
        }

        logger.log(Level.INFO, "Dice Chess Java Bot initialized and listening on port {0} at path {1}", port, DEFAULT_WEBHOOK_PATH);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.log(Level.INFO, "Shutting down Java Bot server...");
            server.stop(1);
            evaluator.close();
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Resolves the webhook keys from system environment variables.
     * Falls back to a placeholder key if neither active nor pending secret is configured.
     *
     * @return the resolved webhook keys
     */
    static WebhookKeys resolveWebhookKeys() {
        return resolveWebhookKeys(System.getenv());
    }

    /**
     * Resolves the webhook keys from the provided environment map.
     *
     * @param env the environment mapping
     * @return the resolved webhook keys
     */
    static WebhookKeys resolveWebhookKeys(Map<String, String> env) {
        try {
            return WebhookKeys.fromEnvironment(env);
        } catch (IllegalArgumentException _) {
            logger.log(Level.WARNING, "Neither {0} nor {1} is configured — using placeholder secret",
                    WebhookKeys.ENV_ACTIVE_SECRET, WebhookKeys.ENV_PENDING_SECRET);
            return WebhookKeys.activeOnly("unconfigured-secret");
        }
    }

    /**
     * Starts the webhook server on an explicit port with configured keys and strategy.
     *
     * @param port the listening port (0 for ephemeral)
     * @param keys the webhook key configuration
     * @param strategy the bot strategy
     * @return the running HTTP server
     * @throws IOException if the server fails to bind
     */
    public static HttpServer start(int port, WebhookKeys keys, Strategy strategy) throws IOException {
        var handler = new WebhookHandler(keys, strategy);
        return CustomHandlerServer.start(port, DEFAULT_WEBHOOK_PATH, handler);
    }

    /**
     * Starts the webhook server with a single active secret.
     *
     * @param port the listening port (0 for ephemeral)
     * @param secret the active secret
     * @param strategy the bot strategy
     * @return the running HTTP server
     * @throws IOException if the server fails to bind
     */
    public static HttpServer start(int port, String secret, Strategy strategy) throws IOException {
        return start(port, WebhookKeys.activeOnly(secret), strategy);
    }

    /**
     * Resolves the server port from the PORT environment variable.
     * Falls back to 8080 if PORT is not set or is invalid.
     *
     * @return the resolved port number
     */
    private static int resolvePort() {
        var portStr = System.getenv("PORT");
        if (portStr != null && !portStr.isBlank()) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException _) {
                logger.log(Level.WARNING, "Invalid PORT environment variable ''{0}'', falling back to 8080", portStr);
            }
        }
        return 8080;
    }
}
