package io.gentics.mesh.mcp.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gentics.mesh.mcp.TestFixtures;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.mesh.MeshAuth;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import io.gentics.mesh.mcp.domain.model.MeshNode;
import io.gentics.mesh.mcp.domain.model.NodeReference;
import io.gentics.mesh.mcp.mesh.MeshClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GetNodeUseCase}.
 */
@ExtendWith(MockitoExtension.class)
class GetNodeUseCaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    MeshClient mockClient;

    @Test
    void execute_projectNotAllowed_throwsForbiddenWithoutCallingClient() {
        var config = TestFixtures.readOnlyConfig(); // only "demo" allowed
        var useCase = new GetNodeUseCase(mockClient, config);

        AppError err = assertThrows(AppError.class,
                () -> useCase.execute("forbidden-project", "uuid-1", "de", MeshAuth.NONE));

        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "A non-allowlisted project must yield FORBIDDEN");
        verify(mockClient, never()).getNode(any(), any(), any(), any());
    }

    @Test
    void execute_languageNotAllowed_throwsForbiddenWithoutCallingClient() {
        var config = TestFixtures.readOnlyConfig(); // only "de", "en"
        var useCase = new GetNodeUseCase(mockClient, config);

        AppError err = assertThrows(AppError.class,
                () -> useCase.execute("demo", "uuid-1", "fr", MeshAuth.NONE));

        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "A non-allowlisted language must yield FORBIDDEN");
        verify(mockClient, never()).getNode(any(), any(), any(), any());
    }

    @Test
    void execute_validInput_returnsNodeJson() throws Exception {
        var config  = TestFixtures.readOnlyConfig();
        var useCase = new GetNodeUseCase(mockClient, config);
        var node    = buildTestNode();

        when(mockClient.getNode("demo", "uuid-abc", "de", MeshAuth.NONE)).thenReturn(node);

        String json = useCase.execute("demo", "uuid-abc", "de", MeshAuth.NONE);
        JsonNode root = MAPPER.readTree(json);

        assertEquals("uuid-abc",    root.get("uuid").asText());
        assertEquals("demo",        root.get("project").asText());
        assertEquals("de",          root.get("language").asText());
        assertEquals("article",     root.get("schema").asText());
        assertEquals("Test Node",   root.get("displayName").asText());
        assertEquals("/test-path",  root.get("path").asText());
        assertTrue(root.get("published").asBoolean(), "published flag must be true");
        assertEquals("1.0",         root.get("version").asText());
    }

    @Test
    void execute_nodeWithFields_fieldsPresent() throws Exception {
        var config  = TestFixtures.readOnlyConfig();
        var useCase = new GetNodeUseCase(mockClient, config);
        var node    = buildTestNode();

        when(mockClient.getNode("demo", "uuid-abc", "de", MeshAuth.NONE)).thenReturn(node);

        String json = useCase.execute("demo", "uuid-abc", "de", MeshAuth.NONE);
        JsonNode root = MAPPER.readTree(json);

        assertTrue(root.has("fields"), "Response JSON must contain a 'fields' object");
        assertEquals("Hello World", root.get("fields").get("title").asText());
    }

    @Test
    void execute_nodeWithReferences_referencesPresent() throws Exception {
        var config  = TestFixtures.readOnlyConfig();
        var useCase = new GetNodeUseCase(mockClient, config);
        var node    = buildTestNodeWithRef();

        when(mockClient.getNode("demo", "uuid-ref", "de", MeshAuth.NONE)).thenReturn(node);

        String json = useCase.execute("demo", "uuid-ref", "de", MeshAuth.NONE);
        JsonNode root = MAPPER.readTree(json);

        JsonNode refs = root.get("references");
        assertEquals(1, refs.size());
        assertEquals("author",      refs.get(0).get("field").asText());
        assertEquals("author-uuid", refs.get(0).get("uuid").asText());
    }

    @Test
    void execute_outputIsValidJson() throws Exception {
        var config  = TestFixtures.readOnlyConfig();
        var useCase = new GetNodeUseCase(mockClient, config);
        when(mockClient.getNode(any(), any(), any(), any())).thenReturn(buildTestNode());

        String json = useCase.execute("demo", "uuid-abc", "de", MeshAuth.NONE);
        assertDoesNotThrow(() -> MAPPER.readTree(json), "execute() must return valid JSON");
    }

    @Test
    void toJson_allFieldsPresent() throws Exception {
        MeshNode node = buildTestNode();
        var objectNode = GetNodeUseCase.toJson(node);
        assertNotNull(objectNode.get("uuid"),        "toJson() must include 'uuid'");
        assertNotNull(objectNode.get("project"),     "toJson() must include 'project'");
        assertNotNull(objectNode.get("language"),    "toJson() must include 'language'");
        assertNotNull(objectNode.get("schema"),      "toJson() must include 'schema'");
        assertNotNull(objectNode.get("displayName"), "toJson() must include 'displayName'");
        assertNotNull(objectNode.get("path"),        "toJson() must include 'path'");
        assertNotNull(objectNode.get("published"),   "toJson() must include 'published'");
        assertNotNull(objectNode.get("version"),     "toJson() must include 'version'");
        assertNotNull(objectNode.get("fields"),      "toJson() must include 'fields'");
        assertNotNull(objectNode.get("references"),  "toJson() must include 'references'");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private MeshNode buildTestNode() {
        return new MeshNode(
                "uuid-abc", "demo", "de", "article",
                "Test Node", "/test-path", true, "1.0",
                Map.of("title", "Hello World"),
                List.of()
        );
    }

    private MeshNode buildTestNodeWithRef() {
        return new MeshNode(
                "uuid-ref", "demo", "de", "article",
                "Ref Node", "/ref-path", true, "2.0",
                Map.of("title", "With Ref"),
                List.of(new NodeReference("author", "author-uuid"))
        );
    }
}
