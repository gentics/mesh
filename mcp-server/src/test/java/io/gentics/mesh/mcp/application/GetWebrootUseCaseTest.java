package io.gentics.mesh.mcp.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gentics.mesh.mcp.TestFixtures;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import io.gentics.mesh.mcp.domain.model.MeshNode;
import io.gentics.mesh.mcp.mesh.MeshAuth;
import io.gentics.mesh.mcp.mesh.MeshClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GetWebrootUseCase}.
 */
@ExtendWith(MockitoExtension.class)
class GetWebrootUseCaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    MeshClient mockClient;

    @Test
    void execute_projectNotAllowed_throwsForbidden() {
        var useCase = new GetWebrootUseCase(mockClient, TestFixtures.readOnlyConfig());

        AppError err = assertThrows(AppError.class,
                () -> useCase.execute("secret", "/path", "de", MeshAuth.NONE));

        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "A non-allowlisted project must yield FORBIDDEN");
        verify(mockClient, never()).getWebroot(any(), any(), any(), any());
    }

    @Test
    void execute_languageNotAllowed_throwsForbidden() {
        var useCase = new GetWebrootUseCase(mockClient, TestFixtures.readOnlyConfig());

        AppError err = assertThrows(AppError.class,
                () -> useCase.execute("demo", "/path", "ru", MeshAuth.NONE));

        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "A non-allowlisted language must yield FORBIDDEN");
        verify(mockClient, never()).getWebroot(any(), any(), any(), any());
    }

    @Test
    void execute_validInput_returnsNodeJson() throws Exception {
        var useCase = new GetWebrootUseCase(mockClient, TestFixtures.readOnlyConfig());
        MeshNode node = new MeshNode(
                "node-uuid", "demo", "de", "article",
                "My Page", "/my-page", true, "1.0",
                Map.of("title", "My Page Title"), List.of()
        );
        when(mockClient.getWebroot("demo", "/my-page", "de", MeshAuth.NONE)).thenReturn(node);

        String json = useCase.execute("demo", "/my-page", "de", MeshAuth.NONE);
        JsonNode root = MAPPER.readTree(json);

        assertEquals("node-uuid", root.get("uuid").asText());
        assertEquals("demo",      root.get("project").asText());
        assertEquals("/my-page",  root.get("path").asText());
    }

    @Test
    void execute_delegatesToClientWithCorrectArgs() throws Exception {
        var useCase = new GetWebrootUseCase(mockClient, TestFixtures.readOnlyConfig());
        MeshNode node = new MeshNode("x", "demo", "en", "a", "d", "/p", true, "1", Map.of(), List.of());
        when(mockClient.getWebroot("demo", "/articles/news", "en", MeshAuth.NONE)).thenReturn(node);

        useCase.execute("demo", "/articles/news", "en", MeshAuth.NONE);
        verify(mockClient).getWebroot("demo", "/articles/news", "en", MeshAuth.NONE);
    }

    @Test
    void execute_outputIsValidJson() throws Exception {
        var useCase = new GetWebrootUseCase(mockClient, TestFixtures.readOnlyConfig());
        MeshNode node = new MeshNode("x", "demo", "de", "a", "d", "/p", true, "1", Map.of(), List.of());
        when(mockClient.getWebroot(any(), any(), any(), any())).thenReturn(node);

        String json = useCase.execute("demo", "/path", "de", MeshAuth.NONE);
        assertDoesNotThrow(() -> MAPPER.readTree(json), "execute() must return valid JSON");
    }
}
