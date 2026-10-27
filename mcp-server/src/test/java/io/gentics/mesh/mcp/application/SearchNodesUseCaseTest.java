package io.gentics.mesh.mcp.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gentics.mesh.mcp.TestFixtures;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import io.gentics.mesh.mcp.domain.model.SearchResult;
import io.gentics.mesh.mcp.mesh.MeshAuth;
import io.gentics.mesh.mcp.mesh.MeshClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SearchNodesUseCase}.
 */
@ExtendWith(MockitoExtension.class)
class SearchNodesUseCaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    MeshClient mockClient;

    // ── Policy checks ─────────────────────────────────────────────────────

    @Test
    void execute_projectNotAllowed_throwsForbidden() {
        var useCase = new SearchNodesUseCase(mockClient, TestFixtures.readOnlyConfig());

        AppError err = assertThrows(AppError.class,
                () -> useCase.execute("blocked", "test", null, "de",
                        List.of("title"), 5, true, MeshAuth.NONE));

        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "A non-allowlisted project must yield FORBIDDEN");
        verify(mockClient, never()).searchNodes(any(), any(), any(), any(), any(), anyInt(), anyBoolean(), any());
    }

    @Test
    void execute_languageNotAllowed_throwsForbidden() {
        var useCase = new SearchNodesUseCase(mockClient, TestFixtures.readOnlyConfig());

        AppError err = assertThrows(AppError.class,
                () -> useCase.execute("demo", "test", null, "zh",
                        List.of("title"), 5, true, MeshAuth.NONE));

        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "A non-allowlisted language must yield FORBIDDEN");
    }

    // ── Limit validation ──────────────────────────────────────────────────

    @Test
    void execute_limitBelowOne_throwsValidationError() {
        var useCase = new SearchNodesUseCase(mockClient, TestFixtures.readOnlyConfig());

        AppError err = assertThrows(AppError.class,
                () -> useCase.execute("demo", "test", null, "de",
                        List.of(), 0, true, MeshAuth.NONE));

        assertEquals(ErrorCode.VALIDATION_ERROR, err.getCode(),
                "limit=0 must yield VALIDATION_ERROR");
        verify(mockClient, never()).searchNodes(any(), any(), any(), any(), any(), anyInt(), anyBoolean(), any());
    }

    @Test
    void execute_limitAbove20_throwsValidationError() {
        var useCase = new SearchNodesUseCase(mockClient, TestFixtures.readOnlyConfig());

        AppError err = assertThrows(AppError.class,
                () -> useCase.execute("demo", "test", null, "de",
                        List.of(), 21, true, MeshAuth.NONE));

        assertEquals(ErrorCode.VALIDATION_ERROR, err.getCode(),
                "limit=21 (above maximum 20) must yield VALIDATION_ERROR");
        assertTrue(err.getMessage().contains("21"),
                "Error message must mention the rejected limit value");
    }

    @Test
    void execute_limitExactly1_isValid() throws Exception {
        var useCase = new SearchNodesUseCase(mockClient, TestFixtures.readOnlyConfig());
        when(mockClient.searchNodes(any(), any(), any(), any(), any(), eq(1), anyBoolean(), any()))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> useCase.execute("demo", "test", null, "de",
                List.of("title"), 1, true, MeshAuth.NONE),
                "limit=1 (minimum) must be accepted");
    }

    @Test
    void execute_limitExactly20_isValid() throws Exception {
        var useCase = new SearchNodesUseCase(mockClient, TestFixtures.readOnlyConfig());
        when(mockClient.searchNodes(any(), any(), any(), any(), any(), eq(20), anyBoolean(), any()))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> useCase.execute("demo", "test", null, "de",
                List.of("title"), 20, true, MeshAuth.NONE),
                "limit=20 (maximum) must be accepted");
    }

    // ── Result serialisation ──────────────────────────────────────────────

    @Test
    void execute_returnsResultsAsJson() throws Exception {
        var useCase = new SearchNodesUseCase(mockClient, TestFixtures.readOnlyConfig());
        when(mockClient.searchNodes(any(), any(), any(), any(), any(), anyInt(), anyBoolean(), any()))
                .thenReturn(List.of(
                        new SearchResult("uuid-1", "Article 1", "article", "de",
                                "/path/1", "snippet one", true),
                        new SearchResult("uuid-2", "Article 2", "blog",    "de",
                                "/path/2", "snippet two", false)
                ));

        String json = useCase.execute("demo", "query", null, "de",
                List.of("title"), 5, true, MeshAuth.NONE);
        JsonNode root = MAPPER.readTree(json);

        assertTrue(root.has("results"), "Response JSON must contain a 'results' array");
        JsonNode results = root.get("results");
        assertEquals(2, results.size());

        assertEquals("uuid-1",      results.get(0).get("uuid").asText());
        assertEquals("Article 1",   results.get(0).get("title").asText());
        assertEquals("article",     results.get(0).get("schema").asText());
        assertEquals("de",          results.get(0).get("language").asText());
        assertEquals("/path/1",     results.get(0).get("path").asText());
        assertEquals("snippet one", results.get(0).get("snippet").asText());
        assertTrue(results.get(0).get("published").asBoolean(), "first result must be marked published");
    }

    @Test
    void execute_emptyResults_returnsEmptyArray() throws Exception {
        var useCase = new SearchNodesUseCase(mockClient, TestFixtures.readOnlyConfig());
        when(mockClient.searchNodes(any(), any(), any(), any(), any(), anyInt(), anyBoolean(), any()))
                .thenReturn(List.of());

        String json = useCase.execute("demo", "nothing", null, "de",
                List.of("title"), 5, true, MeshAuth.NONE);
        JsonNode root = MAPPER.readTree(json);
        assertTrue(root.get("results").isEmpty(), "results array must be empty when client returns no results");
    }

    @Test
    void execute_passesAllParametersToClient() throws Exception {
        var useCase = new SearchNodesUseCase(mockClient, TestFixtures.readOnlyConfig());
        when(mockClient.searchNodes(any(), any(), any(), any(), any(), anyInt(), anyBoolean(), any()))
                .thenReturn(List.of());

        List<String> fields = List.of("title", "teaser");
        useCase.execute("demo", "mein query", "article", "en", fields, 10, false, MeshAuth.NONE);

        verify(mockClient).searchNodes(
                eq("demo"), eq("mein query"), eq("article"),
                eq("en"), eq(fields), eq(10), eq(false), eq(MeshAuth.NONE));
    }
}
