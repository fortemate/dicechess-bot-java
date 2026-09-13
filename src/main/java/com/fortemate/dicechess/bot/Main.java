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
import java.util.Optional;

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
        var server = startApplication(System.getenv());
        if (server != null) {
            try {
                Thread.currentThread().join();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Starts the application server using configuration from the provided environment map.
     * Aborts and returns null if webhook keys are not configured or binding fails.
     *
     * @param env the environment variables map
     * @return the running HttpServer, or null if initialization aborted
     */
    static HttpServer startApplication(Map<String, String> env) {
        var keysOpt = resolveWebhookKeys(env);
        if (keysOpt.isEmpty()) {
            return null;
        }

        var modelPath = env.getOrDefault("MODEL_PATH", "models/baseline.onnx");
        var port = resolvePort(env.get("PORT"));

        var evaluator = new OnnxEvaluator(modelPath);
        var strategy = new OnnxStrategy(evaluator);

        HttpServer server;
        try {
            server = start(port, keysOpt.get(), strategy);
        } catch (IOException | IllegalArgumentException e) {
            logger.log(Level.ERROR, "Failed to start HTTP server on port {0}: {1}", port, e.getMessage());
            evaluator.close();
            return null;
        }

        logger.log(Level.INFO, "Dice Chess Java Bot initialized and listening on port {0} at path {1}", port, DEFAULT_WEBHOOK_PATH);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.log(Level.INFO, "Shutting down Java Bot server...");
            server.stop(1);
            evaluator.close();
        }));

        return server;
    }

    /**
     * Resolves the webhook keys from system environment variables.
     *
     * @return the resolved webhook keys, or empty if neither active nor pending secret is configured
     */
    static Optional<WebhookKeys> resolveWebhookKeys() {
        return resolveWebhookKeys(System.getenv());
    }

    /**
     * Resolves the webhook keys from the provided environment map.
     * Fails closed by logging an error and returning empty if keys are missing or invalid.
     *
     * @param env the environment mapping
     * @return the resolved webhook keys, or empty if not configured
     */
    static Optional<WebhookKeys> resolveWebhookKeys(Map<String, String> env) {
        try {
            return Optional.of(WebhookKeys.fromEnvironment(env));
        } catch (IllegalArgumentException e) {
            logger.log(Level.ERROR, "Missing or invalid webhook signing keys: {0}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Starts the webhook server on an explicit port with configured keys and strategy,
     * registering default health endpoints.
     *
     * @param port the listening port (0 for ephemeral)
     * @param keys the webhook key configuration
     * @param strategy the bot strategy
     * @return the running HTTP server
     * @throws IOException if the server fails to bind
     */
    public static HttpServer start(int port, WebhookKeys keys, Strategy strategy) throws IOException {
        var handler = new WebhookHandler(keys, strategy);
        var server = CustomHandlerServer.start(port, DEFAULT_WEBHOOK_PATH, handler);
        registerHealthEndpoints(server);
        return server;
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

    static void registerHealthEndpoints(HttpServer server) {
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
    }

    /**
     * Resolves the server port from the given port string.
     * Falls back to 8080 if string is null, blank, or invalid.
     *
     * @param portStr the port string from environment
     * @return the resolved port number
     */
    static int resolvePort(String portStr) {
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
