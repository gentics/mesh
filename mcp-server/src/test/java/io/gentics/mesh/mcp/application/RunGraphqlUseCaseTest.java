package io.gentics.mesh.mcp.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gentics.mesh.mcp.TestFixtures;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import io.gentics.mesh.mcp.mesh.MeshClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RunGraphqlUseCase}.
 */
@ExtendWith(MockitoExtension.class)
class RunGraphqlUseCaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    MeshClient mockClient;

    @Test
    void execute_projectNotAllowed_throwsForbidden() {
        var useCase = new RunGraphqlUseCase(mockClient, TestFixtures.readOnlyConfig());

        AppError err = assertThrows(AppError.class,
                () -> useCase.execute("hidden", "{ nodes { uuid } }", Map.of()));

        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "A non-allowlisted project must yield FORBIDDEN");
        verify(mockClient, never()).runGraphql(any(), any(), any());
    }

    @Test
    void execute_validQuery_returnsDataAndMetaEnvelope() throws Exception {
        var useCase = new RunGraphqlUseCase(mockClient, TestFixtures.readOnlyConfig());
        JsonNode rawResponse = MAPPER.readTree("""
            { "data": { "node": { "uuid": "abc123" } } }
            """);
        when(mockClient.runGraphql(eq("demo"), any(), any())).thenReturn(rawResponse);

        String json = useCase.execute("demo", "{ node(uuid:\"abc123\") { uuid } }", Map.of());
        JsonNode root = MAPPER.readTree(json);

        // Must have "data" wrapper
        assertTrue(root.has("data"), "Response must have 'data' field");
        assertEquals("abc123", root.get("data").get("node").get("uuid").asText());

        // Must have "meta" with both flags
        assertTrue(root.has("meta"), "Response must have 'meta' field");
        assertFalse(root.get("meta").get("truncated").asBoolean(), "meta.truncated must be false");
        assertFalse(root.get("meta").get("timedOut").asBoolean(),   "meta.timedOut must be false");
    }

    @Test
    void execute_metaFlagsAlwaysFalse() throws Exception {
        var useCase = new RunGraphqlUseCase(mockClient, TestFixtures.readOnlyConfig());
        JsonNode rawResponse = MAPPER.readTree("{\"data\":{}}");
        when(mockClient.runGraphql(any(), any(), any())).thenReturn(rawResponse);

        String json = useCase.execute("demo", "{ nodes { uuid } }", Map.of());
        JsonNode meta = MAPPER.readTree(json).get("meta");

        assertFalse(meta.get("truncated").asBoolean(), "truncated must be false");
        assertFalse(meta.get("timedOut").asBoolean(),  "timedOut must be false");
    }

    @Test
    void execute_passesVariablesToClient() throws Exception {
        var useCase = new RunGraphqlUseCase(mockClient, TestFixtures.readOnlyConfig());
        JsonNode rawResponse = MAPPER.readTree("{\"data\":{}}");
        Map<String, Object> vars = Map.of("lang", "de", "limit", 5);
        when(mockClient.runGraphql(eq("demo"), any(), eq(vars))).thenReturn(rawResponse);

        useCase.execute("demo", "{ nodes { uuid } }", vars);

        verify(mockClient).runGraphql(eq("demo"), any(), eq(vars));
    }

    @Test
    void execute_emptyVariables_passedThrough() throws Exception {
        var useCase = new RunGraphqlUseCase(mockClient, TestFixtures.readOnlyConfig());
        JsonNode rawResponse = MAPPER.readTree("{\"data\":{}}");
        when(mockClient.runGraphql(any(), any(), eq(Map.of()))).thenReturn(rawResponse);

        assertDoesNotThrow(() ->
                useCase.execute("demo", "{ nodes { uuid } }", Map.of()),
                "execute() must not throw for an empty variables map");
    }

    @Test
    void execute_outputIsValidJson() throws Exception {
        var useCase = new RunGraphqlUseCase(mockClient, TestFixtures.readOnlyConfig());
        JsonNode rawResponse = MAPPER.readTree("{\"data\":{\"x\":1}}");
        when(mockClient.runGraphql(any(), any(), any())).thenReturn(rawResponse);

        String json = useCase.execute("demo", "{ nodes { uuid } }", Map.of());
        assertDoesNotThrow(() -> MAPPER.readTree(json), "execute() must return valid JSON");
    }

    @Test
    void execute_clientThrowsForbidden_propagates() throws Exception {
        var useCase = new RunGraphqlUseCase(mockClient, TestFixtures.readOnlyConfig());
        when(mockClient.runGraphql(any(), any(), any()))
                .thenThrow(AppError.forbidden("mutation blocked by guardrail"));

        AppError err = assertThrows(AppError.class,
                () -> useCase.execute("demo", "mutation { x }", Map.of()));
        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "FORBIDDEN from the client layer must propagate unchanged");
    }
}
