package io.gentics.mesh.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import io.gentics.mesh.mcp.mesh.MeshClient;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Wires the {@link ToolDispatcher} into the MCP Java SDK's stdio transport.
 *
 * <p>The runner only handles transport plumbing: parsing JSON Schema strings
 * into {@link McpSchema.JsonSchema} instances, building the synchronous
 * {@link McpServer}, and translating between SDK types and the transport-agnostic
 * dispatcher result. All tool semantics live in {@link ToolDispatcher}.
 *
 * <p>For the HTTP transport see {@link HttpMcpServer}.
 *
 * @see ToolDispatcher
 * @see HttpMcpServer
 * @since 0.1.0
 */
public class McpServerRunner {

    private static final Logger log = LoggerFactory.getLogger(McpServerRunner.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ToolDispatcher dispatcher;
    private final String serverVersion;

    public McpServerRunner(MeshConfig config, MeshClient client, String serverVersion) {
        this(new ToolDispatcher(config, client), serverVersion);
    }

    public McpServerRunner(ToolDispatcher dispatcher, String serverVersion) {
        this.dispatcher    = dispatcher;
        this.serverVersion = serverVersion;
    }

    /**
     * Builds and starts the MCP server over stdio.
     *
     * <p>This method blocks until the MCP client closes stdin.
     */
    public void run() {
        var transport = new StdioServerTransportProvider();

        var builder = McpServer.sync(transport)
                .serverInfo("mesh-mcp-server", serverVersion)
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .resources(false, false)
                        .build());

        for (ToolDispatcher.ToolSpec spec : dispatcher.tools()) {
            McpSchema.Tool tool = toMcpTool(spec);
            final String toolName = spec.name();
            builder = builder.tool(tool, (exchange, args) -> dispatchSafely(toolName, args));
        }

        var server = builder.build();
        log.info("Gentics Mesh MCP Server (Java) bereit – warte auf stdio…");
        server.closeGracefully();
    }

    private McpSchema.CallToolResult dispatchSafely(String toolName, Map<String, Object> args) {
        try {
            String json = dispatcher.dispatch(toolName, args);
            return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent(json)), false);
        } catch (AppError e) {
            log.error("Tool '{}' error: {}", toolName, e.toMcpJson());
            return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent(e.toMcpJson())), true);
        } catch (Exception e) {
            AppError ae = new AppError(ErrorCode.INTERNAL_ERROR, e.getMessage());
            log.error("Tool '{}' error: {}", toolName, ae.toMcpJson(), e);
            return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent(ae.toMcpJson())), true);
        }
    }

    private static McpSchema.Tool toMcpTool(ToolDispatcher.ToolSpec spec) {
        try {
            McpSchema.JsonSchema schema = MAPPER.treeToValue(spec.schema(), McpSchema.JsonSchema.class);
            return new McpSchema.Tool(spec.name(), spec.description(), schema);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Invalid schema for tool '" + spec.name() + "': " + e.getMessage(), e);
        }
    }
}
