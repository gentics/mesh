package io.gentics.mesh.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gentics.mesh.mcp.TestFixtures;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.mesh.MeshClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the JSON-RPC dispatcher logic in {@link HttpMcpServer}.
 *
 * <p>These tests bypass the HTTP socket and exercise the dispatcher directly,
 * which keeps them fast and free of network flakiness.
 */
@ExtendWith(MockitoExtension.class)
class HttpMcpServerDispatchTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    MeshClient mockClient;

    private HttpMcpServer server() {
        MeshConfig cfg = TestFixtures.readOnlyConfig();
        Metrics metrics = new Metrics();
        MeshCallGuard guard = new MeshCallGuard(cfg, metrics);
        ToolDispatcher dispatcher = new ToolDispatcher(cfg, mockClient, guard);
        return new HttpMcpServer(cfg, dispatcher, AuthValidator.ALLOW_ALL, metrics, "0.2.0-test");
    }

    @Test
    void initialize_returnsProtocolVersionAndCapabilities() throws Exception {
        JsonNode req = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");
        JsonNode resp = server().dispatchJsonRpc(req);
        assertNotNull(resp);
        assertEquals("2.0", resp.get("jsonrpc").asText());
        assertEquals(1, resp.get("id").asInt());
        JsonNode result = resp.get("result");
        assertNotNull(result);
        assertEquals("2025-03-26", result.get("protocolVersion").asText());
        assertTrue(result.get("capabilities").has("tools"));
        assertEquals("mesh-mcp-server", result.get("serverInfo").get("name").asText());
        assertEquals("0.2.0-test", result.get("serverInfo").get("version").asText());
    }

    @Test
    void toolsList_returnsAllSixTools() throws Exception {
        JsonNode req = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
        JsonNode resp = server().dispatchJsonRpc(req);
        assertNotNull(resp);
        JsonNode arr = resp.get("result").get("tools");
        assertTrue(arr.isArray());
        assertEquals(6, arr.size());
        JsonNode first = arr.get(0);
        assertTrue(first.has("name"));
        assertTrue(first.has("description"));
        assertTrue(first.has("inputSchema"));
    }

    @Test
    void notificationsInitialized_returnsNullEnvelope() throws Exception {
        JsonNode req = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
        JsonNode resp = server().dispatchJsonRpc(req);
        assertNull(resp, "Notifications must produce no response envelope");
    }

    @Test
    void unknownMethod_returnsMethodNotFoundError() throws Exception {
        JsonNode req = MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"id\":99,\"method\":\"foo/bar\"}");
        JsonNode resp = server().dispatchJsonRpc(req);
        assertNotNull(resp);
        assertEquals(-32601, resp.get("error").get("code").asInt());
    }

    @Test
    void toolsCall_unknownTool_returnsIsErrorTrue() throws Exception {
        JsonNode req = MAPPER.readTree(
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"does_not_exist\",\"arguments\":{}}}");
        HttpMcpServer.JsonRpcResponse resp = server().dispatchJsonRpc(req, "test-token");
        assertNotNull(resp.envelope());
        JsonNode result = resp.envelope().get("result");
        assertNotNull(result);
        assertTrue(result.get("isError").asBoolean(), "unknown tool must surface as isError=true");
        // Unknown tool is a VALIDATION_ERROR → stays at HTTP 200 with isError in envelope.
        assertEquals(200, resp.httpStatus());
    }
}
