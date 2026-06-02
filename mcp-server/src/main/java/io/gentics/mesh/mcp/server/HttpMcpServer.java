package io.gentics.mesh.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * MCP server transport over HTTP.
 *
 * <p>Implements JSON-RPC 2.0 over a single {@code POST /mcp} endpoint
 * (with a {@code GET /mcp} stub for clients that probe for streaming) and
 * adds the usual operational endpoints: {@code /healthz}, {@code /readyz},
 * {@code /metrics}.
 *
 * <p>The transport is intentionally <strong>not</strong> a full
 * <em>Streamable HTTP</em> implementation in the MCP 2025-03-26 sense — it
 * does not yet open server-to-client SSE streams. In practice that means it
 * supports every tool-call use case (which is request/response) but not
 * server-initiated progress events. This is enough for OpenAI, LangChain,
 * n8n, Open WebUI, and most private-AI integrations to consume the MCP tools
 * via remote HTTP. A future 0.3.0 may upgrade this to a fully streamable
 * transport.
 *
 * <h2>Supported JSON-RPC methods</h2>
 * <ul>
 *   <li>{@code initialize} – returns capabilities + protocolVersion</li>
 *   <li>{@code notifications/initialized} – accepted, no response</li>
 *   <li>{@code ping} – returns empty result</li>
 *   <li>{@code tools/list} – returns all tool definitions</li>
 *   <li>{@code tools/call} – invokes a tool via {@link ToolDispatcher}</li>
 * </ul>
 *
 * <h2>Auth</h2>
 * Every {@code /mcp} request is checked against the configured
 * {@link AuthValidator}. Missing or invalid {@code Authorization} headers
 * return HTTP 401 with a JSON-RPC error body where applicable.
 *
 * <h2>CORS</h2>
 * Configured via {@code MCP_CORS_ORIGINS}. Preflight {@code OPTIONS} requests
 * are answered with the standard {@code Access-Control-*} headers when the
 * origin is in the allowlist (or when the allowlist is {@code *}).
 *
 * @since 0.2.0
 */
public final class HttpMcpServer {

    private static final Logger log = LoggerFactory.getLogger(HttpMcpServer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PROTOCOL_VERSION = "2025-03-26";
    private static final String SERVER_NAME = "mesh-mcp-server";

    /** Methods that go through auth + dispatcher. /healthz, /readyz, /metrics stay open. */
    private static final String MCP_PATH = "/mcp";

    private final MeshConfig config;
    private final ToolDispatcher dispatcher;
    private final AuthValidator authValidator;
    private final Metrics metrics;
    private final String serverVersion;
    private final Set<String> allowedCorsOrigins;
    private final boolean corsWildcard;

    private HttpServer httpServer;

    public HttpMcpServer(MeshConfig config,
                         ToolDispatcher dispatcher,
                         AuthValidator authValidator,
                         Metrics metrics,
                         String serverVersion) {
        this.config        = config;
        this.dispatcher    = dispatcher;
        this.authValidator = authValidator;
        this.metrics       = metrics;
        this.serverVersion = serverVersion;
        this.allowedCorsOrigins = Set.copyOf(config.mcpCorsOrigins());
        this.corsWildcard = allowedCorsOrigins.contains("*");
    }

    /**
     * Binds the server and blocks the current thread until the JVM shuts down.
     */
    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(config.mcpHttpHost(), config.mcpHttpPort()), 0);
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        httpServer.createContext(MCP_PATH, this::handleMcp);
        httpServer.createContext("/healthz", this::handleHealthz);
        httpServer.createContext("/readyz", this::handleReadyz);
        httpServer.createContext("/metrics", this::handleMetrics);

        httpServer.start();
        log.info("MCP HTTP transport listening on http://{}:{}{}  (auth={})",
                config.mcpHttpHost(), config.mcpHttpPort(), MCP_PATH,
                config.mcpAuthDisabled() ? "DISABLED" : "bearer");

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "mcp-http-shutdown"));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        if (httpServer != null) {
            log.info("Stopping MCP HTTP transport…");
            httpServer.stop(2);
        }
    }

    // ── /mcp ───────────────────────────────────────────────────────────────

    private void handleMcp(HttpExchange ex) throws IOException {
        metrics.incrementHttpRequests();
        try {
            String method = ex.getRequestMethod();
            applyCorsHeaders(ex);

            if ("OPTIONS".equalsIgnoreCase(method)) {
                respondNoBody(ex, 204);
                return;
            }
            if ("GET".equalsIgnoreCase(method)) {
                // SSE not yet implemented – signal Method Not Allowed cleanly.
                ex.getResponseHeaders().add("Allow", "POST, OPTIONS");
                respondText(ex, 405, "GET /mcp not supported in this release. Use POST for JSON-RPC.");
                return;
            }
            if (!"POST".equalsIgnoreCase(method)) {
                respondText(ex, 405, "Method Not Allowed");
                return;
            }

            String authHeader = ex.getRequestHeaders().getFirst("Authorization");
            if (!authValidator.isAuthorized(authHeader)) {
                metrics.incrementHttpAuthFailed();
                ex.getResponseHeaders().add("WWW-Authenticate", "Bearer realm=\"mcp\"");
                respondText(ex, 401, "Unauthorized");
                return;
            }
            String tokenKey = config.mcpAuthDisabled() ? "no-auth" : tokenKeyFromHeader(authHeader);

            JsonNode request;
            try (InputStream is = ex.getRequestBody()) {
                request = MAPPER.readTree(is);
            } catch (Exception parseEx) {
                respondJson(ex, 400, jsonRpcError(null, -32700, "Parse error: " + parseEx.getMessage()));
                return;
            }
            if (request == null || request.isMissingNode() || request.isNull()) {
                respondJson(ex, 400, jsonRpcError(null, -32600, "Empty request body"));
                return;
            }

            JsonRpcResponse response = dispatchJsonRpc(request, tokenKey);
            if (response.envelope() == null) {
                // Notification – no response per JSON-RPC 2.0 spec.
                respondNoBody(ex, 204);
            } else {
                if (response.retryAfterSeconds() != null) {
                    ex.getResponseHeaders().add("Retry-After", String.valueOf(response.retryAfterSeconds()));
                }
                respondJson(ex, response.httpStatus(), response.envelope());
            }
        } catch (Exception unexpected) {
            log.error("Unhandled error in /mcp handler", unexpected);
            try {
                respondJson(ex, 500, jsonRpcError(null, -32603, "Internal error: " + unexpected.getMessage()));
            } catch (IOException ignore) { /* connection probably gone */ }
        } finally {
            ex.close();
        }
    }

    /** Carrier for a JSON-RPC envelope plus HTTP-level signalling. */
    public record JsonRpcResponse(JsonNode envelope, int httpStatus, Integer retryAfterSeconds) {
        static JsonRpcResponse ok(JsonNode env) { return new JsonRpcResponse(env, 200, null); }
        static JsonRpcResponse status(JsonNode env, int code, int retry) { return new JsonRpcResponse(env, code, retry); }
        static JsonRpcResponse notification() { return new JsonRpcResponse(null, 204, null); }
    }

    /** Test/back-compat helper – returns only the envelope, treats all errors as 200. */
    JsonNode dispatchJsonRpc(JsonNode request) {
        return dispatchJsonRpc(request, "stdio").envelope();
    }

    /** Dispatches a JSON-RPC request and returns the envelope + HTTP status hints. */
    JsonRpcResponse dispatchJsonRpc(JsonNode request, String tokenKey) {
        JsonNode idNode = request.get("id");
        String method = request.path("method").asText("");
        JsonNode params = request.get("params");

        boolean isNotification = idNode == null || idNode.isNull();

        try {
            return switch (method) {
                case "initialize" -> JsonRpcResponse.ok(jsonRpcResult(idNode, buildInitializeResult(params)));
                case "notifications/initialized", "initialized" -> JsonRpcResponse.notification();
                case "ping" -> JsonRpcResponse.ok(jsonRpcResult(idNode, MAPPER.createObjectNode()));
                case "tools/list" -> JsonRpcResponse.ok(jsonRpcResult(idNode, buildToolsList()));
                case "tools/call" -> dispatchToolsCall(idNode, params, tokenKey);
                default -> isNotification
                        ? JsonRpcResponse.notification()
                        : JsonRpcResponse.ok(jsonRpcError(idNode, -32601, "Method not found: " + method));
            };
        } catch (Exception e) {
            if (isNotification) return JsonRpcResponse.notification();
            return JsonRpcResponse.ok(jsonRpcError(idNode, -32603, "Internal error: " + e.getMessage()));
        }
    }

    private ObjectNode buildInitializeResult(JsonNode params) {
        ObjectNode result = MAPPER.createObjectNode();
        result.put("protocolVersion", PROTOCOL_VERSION);

        ObjectNode capabilities = MAPPER.createObjectNode();
        ObjectNode tools = MAPPER.createObjectNode();
        tools.put("listChanged", false);
        capabilities.set("tools", tools);
        result.set("capabilities", capabilities);

        ObjectNode serverInfo = MAPPER.createObjectNode();
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("version", serverVersion);
        result.set("serverInfo", serverInfo);
        return result;
    }

    private ObjectNode buildToolsList() {
        ObjectNode result = MAPPER.createObjectNode();
        ArrayNode arr = result.putArray("tools");
        for (ToolDispatcher.ToolSpec spec : dispatcher.tools()) {
            ObjectNode t = arr.addObject();
            t.put("name", spec.name());
            t.put("description", spec.description());
            t.set("inputSchema", spec.schema());
        }
        return result;
    }

    private JsonRpcResponse dispatchToolsCall(JsonNode idNode, JsonNode params, String tokenKey) {
        if (params == null) {
            return JsonRpcResponse.ok(jsonRpcError(idNode, -32602, "Missing params"));
        }
        String name = params.path("name").asText("");
        if (name.isBlank()) {
            return JsonRpcResponse.ok(jsonRpcError(idNode, -32602, "Missing tool name"));
        }
        Map<String, Object> args;
        JsonNode argsNode = params.get("arguments");
        if (argsNode == null || argsNode.isMissingNode() || argsNode.isNull()) {
            args = Map.of();
        } else if (argsNode.isObject()) {
            args = MAPPER.convertValue(argsNode, Map.class);
        } else {
            return JsonRpcResponse.ok(jsonRpcError(idNode, -32602, "tools/call 'arguments' must be an object"));
        }

        long start = System.currentTimeMillis();
        boolean error = false;
        ErrorCode errorCode = null;
        String resultJson;
        try {
            resultJson = dispatcher.dispatch(name, args, tokenKey);
        } catch (AppError e) {
            error = true;
            errorCode = e.getCode();
            resultJson = e.toMcpJson();
        } catch (Exception e) {
            error = true;
            errorCode = ErrorCode.INTERNAL_ERROR;
            resultJson = new AppError(ErrorCode.INTERNAL_ERROR, e.getMessage()).toMcpJson();
        } finally {
            metrics.recordToolCall(name, System.currentTimeMillis() - start, error);
        }

        ObjectNode result = MAPPER.createObjectNode();
        ArrayNode content = result.putArray("content");
        ObjectNode textBlock = content.addObject();
        textBlock.put("type", "text");
        textBlock.put("text", resultJson);
        result.put("isError", error);

        JsonNode envelope = jsonRpcResult(idNode, result);
        return toHttpStatus(envelope, errorCode);
    }

    /**
     * Maps protection-layer errors to proper HTTP status + Retry-After hints.
     * Everything else stays at HTTP 200 with isError in the JSON-RPC envelope.
     */
    private JsonRpcResponse toHttpStatus(JsonNode envelope, ErrorCode code) {
        if (code == null) return JsonRpcResponse.ok(envelope);
        return switch (code) {
            case RATE_LIMITED, BUSY -> JsonRpcResponse.status(envelope, 429, 1);
            case CIRCUIT_OPEN       -> JsonRpcResponse.status(envelope, 503,
                    Math.max(1, (int) Math.ceil(config.mcpCircuitBreakerOpenDurationMs() / 1000.0)));
            default                 -> JsonRpcResponse.ok(envelope);
        };
    }

    /**
     * Computes a stable key derived from the Authorization header. SHA-256
     * keeps the bearer token out of memory in plaintext (the limiter map
     * keys would otherwise leak it via heap dumps).
     */
    private static String tokenKeyFromHeader(String authHeader) {
        if (authHeader == null) return "anon";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(authHeader.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return "anon";
        }
    }

    // ── operational endpoints ───────────────────────────────────────────────

    private void handleHealthz(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                respondText(ex, 405, "Method Not Allowed");
                return;
            }
            respondText(ex, 200, "ok");
        } finally {
            ex.close();
        }
    }

    private void handleReadyz(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                respondText(ex, 405, "Method Not Allowed");
                return;
            }
            // Cheap readiness: server is ready as soon as it has bound the port.
            // A heavier Mesh ping would belong in a separate /readyz-deep endpoint.
            respondText(ex, 200, "ready");
        } finally {
            ex.close();
        }
    }

    private void handleMetrics(HttpExchange ex) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                respondText(ex, 405, "Method Not Allowed");
                return;
            }
            byte[] body = metrics.renderPrometheus().getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        } finally {
            ex.close();
        }
    }

    // ── JSON-RPC + HTTP helpers ────────────────────────────────────────────

    private ObjectNode jsonRpcResult(JsonNode idNode, JsonNode result) {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("jsonrpc", "2.0");
        env.set("id", idNode == null ? null : idNode.deepCopy());
        env.set("result", result);
        return env;
    }

    private ObjectNode jsonRpcError(JsonNode idNode, int code, String message) {
        ObjectNode env = MAPPER.createObjectNode();
        env.put("jsonrpc", "2.0");
        env.set("id", idNode == null ? null : idNode.deepCopy());
        ObjectNode err = env.putObject("error");
        err.put("code", code);
        err.put("message", message);
        return env;
    }

    private void respondJson(HttpExchange ex, int status, JsonNode body) throws IOException {
        byte[] bytes = MAPPER.writeValueAsBytes(body);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private void respondText(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private void respondNoBody(HttpExchange ex, int status) throws IOException {
        ex.sendResponseHeaders(status, -1);
    }

    private void applyCorsHeaders(HttpExchange ex) {
        if (allowedCorsOrigins.isEmpty()) return;
        String origin = ex.getRequestHeaders().getFirst("Origin");
        Headers out = ex.getResponseHeaders();
        if (corsWildcard) {
            out.add("Access-Control-Allow-Origin", "*");
        } else if (origin != null && allowedCorsOrigins.contains(origin)) {
            out.add("Access-Control-Allow-Origin", origin);
            out.add("Vary", "Origin");
        } else {
            return; // origin not allowed → emit no CORS headers
        }
        out.add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        out.add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        out.add("Access-Control-Max-Age", "86400");
    }

    // exposed for tests
    List<ToolDispatcher.ToolSpec> tools() { return dispatcher.tools(); }
}
