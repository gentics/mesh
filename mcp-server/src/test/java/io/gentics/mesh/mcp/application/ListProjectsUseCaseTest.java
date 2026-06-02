package io.gentics.mesh.mcp.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gentics.mesh.mcp.TestFixtures;
import io.gentics.mesh.mcp.domain.model.Project;
import io.gentics.mesh.mcp.mesh.MeshClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ListProjectsUseCase}.
 */
@ExtendWith(MockitoExtension.class)
class ListProjectsUseCaseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    MeshClient mockClient;

    @Test
    void execute_returnsProjectsArrayAsJson() throws Exception {
        when(mockClient.listProjects()).thenReturn(List.of(
                new Project("demo",  "uuid-demo"),
                new Project("shop",  "uuid-shop")
        ));

        var useCase = new ListProjectsUseCase(mockClient, TestFixtures.readOnlyConfig());
        String json = useCase.execute();
        JsonNode root = MAPPER.readTree(json);

        assertTrue(root.has("projects"), "Response JSON must contain a 'projects' array");
        JsonNode projects = root.get("projects");
        assertEquals(2, projects.size());
        assertEquals("demo",      projects.get(0).get("name").asText());
        assertEquals("uuid-demo", projects.get(0).get("uuid").asText());
        assertEquals("shop",      projects.get(1).get("name").asText());
    }

    @Test
    void execute_emptyList_returnsEmptyArray() throws Exception {
        when(mockClient.listProjects()).thenReturn(List.of());

        var useCase = new ListProjectsUseCase(mockClient, TestFixtures.readOnlyConfig());
        String json = useCase.execute();
        JsonNode root = MAPPER.readTree(json);

        assertTrue(root.get("projects").isEmpty(), "projects array must be empty when client returns no projects");
    }

    @Test
    void execute_delegatesToMeshClient() throws Exception {
        when(mockClient.listProjects()).thenReturn(List.of());
        var useCase = new ListProjectsUseCase(mockClient, TestFixtures.readOnlyConfig());
        useCase.execute();
        verify(mockClient, times(1)).listProjects();
    }

    @Test
    void execute_outputIsValidJson() throws Exception {
        when(mockClient.listProjects()).thenReturn(
                List.of(new Project("alpha", "uuid-alpha")));

        var useCase = new ListProjectsUseCase(mockClient, TestFixtures.readOnlyConfig());
        String json = useCase.execute();
        assertDoesNotThrow(() -> MAPPER.readTree(json), "execute() must return valid JSON");
    }
}
