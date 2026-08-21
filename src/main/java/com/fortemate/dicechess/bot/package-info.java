/**
 * Dice Chess Java Bot Starter Template &amp; Reference Implementation.
 *
 * <p>This package provides an end-to-end reference implementation for writing Dice Chess bots
 * in Java 25+. It demonstrates how to interface with the {@code dicechess-bot-runtime} Webhook server,
 * parse game positions and generate legal turns using the {@code dicechess-engine} JvmApi facade,
 * and evaluate positions using an ONNX neural network model or built-in engine heuristics.</p>
 *
 * <h2>Architecture Overview</h2>
 * <ul>
 *     <li>{@link com.fortemate.dicechess.bot.Main}: Application entry point. Configures port, loads secrets from env,
 *         instantiates evaluator and strategy, and launches the HTTP webhook server.</li>
 *     <li>{@link com.fortemate.dicechess.bot.Strategy}: Core functional interface for decision-making logic.
 *         Maps a {@link lv.id.jc.dicechess.runtime.TurnContext} to a list of long algebraic move notations.</li>
 *     <li>{@link com.fortemate.dicechess.bot.OnnxStrategy}: Primary strategy implementation. Parses DFEN via
 *         {@link dicechess.engine.jvmapi.JvmApi}, expands full multi-move turns via {@link dicechess.engine.jvmapi.JvmApi#legalTurns},
 *         and scores candidate positions using {@link com.fortemate.dicechess.bot.OnnxEvaluator}.</li>
 *     <li>{@link com.fortemate.dicechess.bot.OnnxEvaluator}: Manages ONNX Runtime sessions ({@code OrtSession}) to evaluate
 *         board positions via neural network models (e.g. {@code baseline.onnx}). Falls back gracefully
 *         to {@link dicechess.engine.jvmapi.JvmApi#evaluate} engine heuristic evaluation if no model file is provided.</li>
 * </ul>
 *
 * <h2>Environment Variables</h2>
 * <table>
 *     <caption>Environment Configuration</caption>
 *     <tr><th>Variable</th><th>Default</th><th>Description</th></tr>
 *     <tr><td>{@code DICECHESS_WEBHOOK_SECRET}</td><td><em>Empty</em></td><td>HMAC secret key for verifying incoming webhook requests.</td></tr>
 *     <tr><td>{@code MODEL_PATH}</td><td>{@code models/baseline.onnx}</td><td>Path to the ONNX model file on disk.</td></tr>
 *     <tr><td>{@code PORT}</td><td>{@code 8080}</td><td>HTTP server binding port for incoming webhook deliveries.</td></tr>
 * </table>
 *
 * @see lv.id.jc.dicechess.runtime.CustomHandlerServer
 * @see lv.id.jc.dicechess.runtime.WebhookHandler
 */
package com.fortemate.dicechess.bot;
