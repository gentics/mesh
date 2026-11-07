package io.gentics.mesh.mcp.server;

import io.gentics.mesh.mcp.TestFixtures;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.mesh.MeshAuth;
import io.gentics.mesh.mcp.mesh.MeshClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the rule that decides which Mesh identity a request runs under.
 *
 * <p>The {@code Authorization} header is interpreted differently depending on
 * whether the MCP server checks it itself, so the two cases are asserted
 * explicitly – forwarding an MCP token to Mesh would leak a credential that
 * Mesh has no business seeing.
 */
@ExtendWith(MockitoExtension.class)
class MeshAuthForwardingTest {

    @Mock
    MeshClient mockClient;

    private HttpMcpServer server(MeshConfig cfg) {
        Metrics metrics = new Metrics();
        MeshCallGuard guard = new MeshCallGuard(cfg, metrics);
        ToolDispatcher dispatcher = new ToolDispatcher(cfg, mockClient, guard);
        AuthValidator validator = cfg.mcpAuthDisabled()
                ? AuthValidator.ALLOW_ALL
                : new BearerTokenValidator(cfg.mcpAuthToken());
        return new HttpMcpServer(cfg, dispatcher, validator, metrics, "test");
    }

    // ── MCP auth enabled: the token is ours, never Mesh's ─────────────────

    @Test
    void mcpAuthEnabled_bearerHeader_isNotForwardedToMesh() {
        var server = server(TestFixtures.httpConfig(false));
        assertSame(MeshAuth.NONE, server.meshAuthFor("Bearer mcp-server-token"),
                "With MCP auth on, the bearer token authenticates the client to the "
                + "MCP server and must never reach Mesh");
    }

    @Test
    void mcpAuthEnabled_noHeader_isAnonymous() {
        var server = server(TestFixtures.httpConfig(false));
        assertSame(MeshAuth.NONE, server.meshAuthFor(null));
    }

    // ── MCP auth disabled: the token belongs to Mesh ──────────────────────

    @Test
    void mcpAuthDisabled_bearerHeader_isForwardedToMesh() {
        var server = server(TestFixtures.httpConfig(true));
        MeshAuth auth = server.meshAuthFor("Bearer mesh-user-token");
        assertTrue(auth.hasToken());
        assertEquals("mesh-user-token", auth.bearerToken());
    }

    @Test
    void mcpAuthDisabled_noHeader_isAnonymous() {
        var server = server(TestFixtures.httpConfig(true));
        assertSame(MeshAuth.NONE, server.meshAuthFor(null));
    }

    @Test
    void mcpAuthDisabled_nonBearerScheme_isAnonymous() {
        var server = server(TestFixtures.httpConfig(true));
        assertSame(MeshAuth.NONE, server.meshAuthFor("Basic dXNlcjpwdw=="),
                "Only Bearer tokens are meaningful to Mesh");
    }

    @Test
    void mcpAuthDisabled_bearerSchemeIsCaseInsensitive() {
        var server = server(TestFixtures.httpConfig(true));
        assertEquals("abc", server.meshAuthFor("bearer abc").bearerToken());
    }

    // ── the identity actually reaches the Mesh client ─────────────────────

    @Test
    void dispatch_passesMeshAuthThroughToTheClient() {
        MeshConfig cfg = TestFixtures.httpConfig(true);
        MeshCallGuard guard = new MeshCallGuard(cfg, new Metrics());
        ToolDispatcher dispatcher = new ToolDispatcher(cfg, mockClient, guard);
        MeshAuth auth = MeshAuth.bearer("mesh-user-token");

        when(mockClient.listProjects(auth)).thenReturn(List.of());
        dispatcher.dispatch("mesh_list_projects", Map.of(), "token-key", auth);

        verify(mockClient).listProjects(auth);
    }

    @Test
    void dispatch_stdioConvenienceOverload_usesNoPerCallIdentity() {
        MeshConfig cfg = TestFixtures.readOnlyConfig();
        MeshCallGuard guard = new MeshCallGuard(cfg, new Metrics());
        ToolDispatcher dispatcher = new ToolDispatcher(cfg, mockClient, guard);

        when(mockClient.listProjects(MeshAuth.NONE)).thenReturn(List.of());
        dispatcher.dispatch("mesh_list_projects", Map.of());

        verify(mockClient).listProjects(MeshAuth.NONE);
    }

    // ── MeshAuth itself ───────────────────────────────────────────────────

    @Test
    void bearer_blankOrNullToken_collapsesToNone() {
        assertSame(MeshAuth.NONE, MeshAuth.bearer(null));
        assertSame(MeshAuth.NONE, MeshAuth.bearer("   "));
        assertFalse(MeshAuth.NONE.hasToken());
        assertNull(MeshAuth.NONE.bearerToken());
    }

    @Test
    void bearer_trimsSurroundingWhitespace() {
        assertEquals("tok", MeshAuth.bearer("  tok  ").bearerToken());
    }
}
