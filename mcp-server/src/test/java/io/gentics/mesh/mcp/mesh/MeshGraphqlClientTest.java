package io.gentics.mesh.mcp.mesh;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.gentics.mesh.mcp.TestFixtures;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MeshGraphqlClient}.
 *
 * <p>The static helpers {@code isMutation()} and {@code measureDepth()} are
 * tested directly without any mocking. Integration guardrails (mutation guard,
 * depth guard) are verified via a mocked {@link MeshRestClient}: if a guard
 * fires, the underlying {@code graphql()} call must never be made.
 *
 * <p>Note: {@code MeshRestClient.graphql()} returns {@code MeshRequest<GraphQLResponse>},
 * not a raw RxJava {@code Single}. Tests mock {@link MeshRequest} accordingly.
 */
@ExtendWith(MockitoExtension.class)
class MeshGraphqlClientTest {

    @Mock
    MeshRestClient mockClient;

    /** Creates a stub MeshRequest that immediately returns the given response. */
    @SuppressWarnings("unchecked")
    private static MeshRequest<GraphQLResponse> stubRequest(GraphQLResponse response) {
        MeshRequest<GraphQLResponse> req = mock(MeshRequest.class);
        when(req.blockingGet()).thenReturn(response);
        return req;
    }

    // ══════════════════════════════════════════════════════════════════════
    // isMutation()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void isMutation_query_returnsFalse() {
        assertFalse(MeshGraphqlClient.isMutation("{ nodes { uuid } }"));
    }

    @Test
    void isMutation_explicitQueryKeyword_returnsFalse() {
        assertFalse(MeshGraphqlClient.isMutation("query { node(uuid:\"x\") { uuid } }"));
    }

    @Test
    void isMutation_mutationKeyword_returnsTrue() {
        assertTrue(MeshGraphqlClient.isMutation("mutation { createNode { uuid } }"));
    }

    @Test
    void isMutation_mutationWithLeadingWhitespace_returnsTrue() {
        assertTrue(MeshGraphqlClient.isMutation("  \n  mutation { x }"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MUTATION { x }", "Mutation { x }", "MuTaTiOn { x }"})
    void isMutation_caseInsensitive_returnsTrue(String query) {
        assertTrue(MeshGraphqlClient.isMutation(query),
                "Should detect mutation keyword regardless of case: " + query);
    }

    @Test
    void isMutation_fieldNamedMutationResult_returnsFalse() {
        assertFalse(MeshGraphqlClient.isMutation("{ mutationResult { uuid } }"));
    }

    @Test
    void isMutation_fragmentWithMutation_returnsTrue() {
        assertTrue(MeshGraphqlClient.isMutation(
                "mutation CreateNode($input: NodeCreateInput!) { createNode(input: $input) { uuid } }"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // measureDepth()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void measureDepth_emptyString_returnsZero() {
        assertEquals(0, MeshGraphqlClient.measureDepth(""));
    }

    @Test
    void measureDepth_noBraces_returnsZero() {
        assertEquals(0, MeshGraphqlClient.measureDepth("query"));
    }

    @Test
    void measureDepth_singleLevel_returnsOne() {
        assertEquals(1, MeshGraphqlClient.measureDepth("{ }"));
    }

    @Test
    void measureDepth_twoLevels_returnsTwo() {
        assertEquals(2, MeshGraphqlClient.measureDepth("{ nodes { uuid } }"));
    }

    @Test
    void measureDepth_fourLevels_returnsFour() {
        assertEquals(4, MeshGraphqlClient.measureDepth("{ a { b { c { d } } } }"));
    }

    @Test
    void measureDepth_fiveLevels_returnsFive() {
        assertEquals(5, MeshGraphqlClient.measureDepth(
                "{ nodes { elements { fields { nested { value } } } } }"));
    }

    @Test
    void measureDepth_unbalancedQuery_countsMaxReached() {
        assertEquals(3, MeshGraphqlClient.measureDepth("{ a { b { c"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // query() – guardrail tests
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void query_mutationAttempt_throwsForbiddenBeforeHttpCall() {
        MeshConfig config = TestFixtures.readOnlyConfig();
        var gqlClient = new MeshGraphqlClient(config, mockClient);

        AppError err = assertThrows(AppError.class,
                () -> gqlClient.query("demo", "mutation { createNode { uuid } }", Map.of()));

        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "A mutation query must be blocked with FORBIDDEN");
        verify(mockClient, never()).graphql(any(), any(GraphQLRequest.class));
    }

    @Test
    void query_depthExceedsLimit_throwsValidationErrorBeforeHttpCall() {
        MeshConfig config = TestFixtures.shallowDepthConfig(2);
        var gqlClient = new MeshGraphqlClient(config, mockClient);

        AppError err = assertThrows(AppError.class,
                () -> gqlClient.query("demo", "{ a { b { c } } }", Map.of()));

        assertEquals(ErrorCode.VALIDATION_ERROR, err.getCode(),
                "A query exceeding the depth limit must yield VALIDATION_ERROR");
        assertTrue(err.getMessage().contains("3"),
                "Error message must mention the actual depth (3)");
        assertTrue(err.getMessage().contains("2"),
                "Error message must mention the configured limit (2)");
        verify(mockClient, never()).graphql(any(), any(GraphQLRequest.class));
    }

    @Test
    void query_validQuery_callsClientGraphqlWithProject() {
        MeshConfig config = TestFixtures.readOnlyConfig();
        var stub = stubRequest(new GraphQLResponse());
        when(mockClient.graphql(eq("demo"), any(GraphQLRequest.class))).thenReturn(stub);

        var gqlClient = new MeshGraphqlClient(config, mockClient);
        var result = gqlClient.query("demo", "{ node(uuid:\"x\") { uuid } }", Map.of());

        assertNotNull(result, "query() must return a non-null JsonNode result");
        verify(mockClient, times(1)).graphql(eq("demo"), any(GraphQLRequest.class));
    }

    @Test
    void query_withVariables_passesVariablesToRequest() {
        MeshConfig config = TestFixtures.readOnlyConfig();
        var stub = stubRequest(new GraphQLResponse());
        when(mockClient.graphql(any(), any(GraphQLRequest.class))).thenReturn(stub);

        var gqlClient = new MeshGraphqlClient(config, mockClient);
        gqlClient.query("demo", "{ nodes { uuid } }", Map.of("lang", "de"));

        verify(mockClient).graphql(
                eq("demo"),
                argThat(req -> req.getVariables() != null
                        && req.getVariables().containsKey("lang"))
        );
    }

    @Test
    void query_withEmptyVariables_omitsVariablesFromRequest() {
        MeshConfig config = TestFixtures.readOnlyConfig();
        var stub = stubRequest(new GraphQLResponse());
        when(mockClient.graphql(any(), any(GraphQLRequest.class))).thenReturn(stub);

        var gqlClient = new MeshGraphqlClient(config, mockClient);
        gqlClient.query("demo", "{ nodes { uuid } }", Map.of());

        verify(mockClient).graphql(
                eq("demo"),
                argThat(req -> req.getVariables() == null
                        || req.getVariables().isEmpty())
        );
    }

    @Test
    void query_exactlyAtDepthLimit_doesNotThrow() {
        MeshConfig config = TestFixtures.shallowDepthConfig(2);
        var stub = stubRequest(new GraphQLResponse());
        when(mockClient.graphql(any(), any(GraphQLRequest.class))).thenReturn(stub);

        var gqlClient = new MeshGraphqlClient(config, mockClient);
        assertDoesNotThrow(() -> gqlClient.query("demo", "{ nodes { uuid } }", Map.of()),
                "A query at exactly the depth limit must not throw");
    }
}
