package com.gentics.mesh.core.graphql;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.*;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

/**
 * Tests for node queries when having a node with published version equals to draft version.
 */
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLPublishedMatchingDraftNodePermissionTest extends AbstractGraphQLNodeTest {

    private final String query;
    private final String version;

    public GraphQLPublishedMatchingDraftNodePermissionTest(String query, String version) {
        this.query = query;
        this.version = version;
    }

    @Parameterized.Parameters(name = "query={0}, version={1}")
    public static Collection<Object[]> parameters() {
        List<String> queries = Arrays.asList(
                "rootNode",
                "nodePerUuid",
                "nodePerPath"
        );
        List<String> versions = Arrays.asList("draft", "published");

        List<Object[]> data = new ArrayList<>();
        for (String query : queries) {
            for (String version : versions) {

                data.add(new Object[]{query, version});
            }
        }

        return data;
    }

    private void setupContent() {
        String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());

        SchemaCreateRequest schemaRequest = new SchemaCreateRequest();
        schemaRequest.setName(SCHEMA_NAME);
        schemaRequest.setContainer(true);
        schemaRequest.setSegmentField("name");
        schemaRequest.addField(FieldUtil.createStringFieldSchema("name"));
        schemaRequest.addField(FieldUtil.createStringFieldSchema("extra"));
        schemaRequest.addField(FieldUtil.createNodeFieldSchema("node"));
        schemaRequest.addField(FieldUtil.createListFieldSchema("nodeList").setListType("node"));

        SchemaResponse schemaResponse = call(() -> client().createSchema(schemaRequest));
        call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));

        NodeUpdateRequest rootNodeUpdateRequest = new NodeUpdateRequest();
        rootNodeUpdateRequest.setLanguage("en");
        rootNodeUpdateRequest.getFields().putString("name", "root");
        call(() -> client().updateNode(PROJECT_NAME, baseNodeUuid, rootNodeUpdateRequest));
        call(() -> client().publishNode(PROJECT_NAME, baseNodeUuid, new PublishParametersImpl().setRecursive(true)));
    }

    public void setupPermissions() {
        // Apply permissions for test run
        RolePermissionRequest permRequest = new RolePermissionRequest();
        PermissionInfo permissionsInfo = permRequest.getPermissions();
        permissionsInfo.set(Permission.READ_PUBLISHED, true);
        permissionsInfo.setOthers(false);
        permRequest.setRecursive(true);
        adminCall(() -> client().updateRolePermissions(roleUuid(), "/projects/" + PROJECT_NAME + "/nodes", permRequest));
    }

    @Test
    public void test() throws IOException {
        setupContent();
        setupPermissions();
        GraphQLRequest request = new GraphQLRequest();
        String queryName = "node/publishedmatchingdraft/" + query;
        JsonObject var = new JsonObject();
        var.put("uuid", tx(() -> project().getBaseNode().getUuid()));
        var.put("type", version);
        request.setVariables(var);
        request.setQuery(getGraphQLQuery(queryName));
        GraphQLResponse response = call(() -> client().graphql(PROJECT_NAME, request));
        JsonObject jsonResponse = new JsonObject(response.toJson());
        System.out.println(jsonResponse.encodePrettily());
        System.out.println("Query: " + queryName);

        compliesToAssertions(jsonResponse, queryName);
    }

    public static final String CHECK_PERM = "checkperm:";

    private void compliesToAssertions(JsonObject jsonResponse, String queryName) throws IOException {
        String query = getGraphQLQuery(queryName);

        try (Scanner scanner = new Scanner(query)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                line = line.trim();
                if (line.startsWith("# [")) {
                    int start = line.indexOf("# [") + 3;
                    int end = line.lastIndexOf("]");
                    String selector = line.substring(start, end);
                    int ab = line.indexOf("=", end) + 1;
                    String assertion = line.substring(ab);
                    String currentSelector = "select_" + version;
                    if (currentSelector.equalsIgnoreCase(selector)) {
                        if (assertion.startsWith(CHECK_PERM)) {
                            String permPath = assertion.substring(CHECK_PERM.length());
                            assertThat(jsonResponse).hasPermFailure(permPath);
                        } else {
                            assertThat(jsonResponse).compliesTo(assertion);
                        }
                    }
                }
            }
        }
    }
}