package io.gentics.mesh.mcp;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.NEWS_UUID;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.search.AbstractNodeSearchEndpointTest;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;

import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import io.gentics.mesh.mcp.mesh.MeshClient;
import io.gentics.mesh.mcp.server.MeshCallGuard;
import io.gentics.mesh.mcp.server.Metrics;
import io.gentics.mesh.mcp.server.ToolDispatcher;

/**
 * Integration test for the MCP tools against a real Mesh instance with
 * Elasticsearch, started by {@link AbstractNodeSearchEndpointTest} once per
 * Elasticsearch version (test data: project {@code dummy}).
 */
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class MeshMcpServerIntegrationTest extends AbstractNodeSearchEndpointTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private String username;
	private String password;
	private ToolDispatcher dispatcher;

	public MeshMcpServerIntegrationTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Before
	public void setupDispatcher() {
		username = tx(() -> user().getUsername());
		password = data().getUserInfo().getPassword();
		dispatcher = dispatcher(List.of(PROJECT_NAME));
	}

	@Test
	public void testLoginWithValidCredentials() {
		// the constructor verifies the login and throws if Mesh answers as anonymous
		new MeshClient(config(username, password, List.of(PROJECT_NAME)));
	}

	@Test
	public void testLoginWithWrongPasswordFails() {
		assertThatThrownBy(() -> new MeshClient(config(username, "wrong-password", List.of(PROJECT_NAME))))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Mesh-Login fehlgeschlagen");
	}

	@Test
	public void testListProjects() throws Exception {
		JsonNode result = dispatch("mesh_list_projects", Map.of());

		assertThat(result.path("projects")).hasSize(1);
		assertThat(result.path("projects").get(0).path("name").asText()).isEqualTo(PROJECT_NAME);
		assertThat(result.path("projects").get(0).path("uuid").asText()).isEqualTo(tx(() -> project().getUuid()));
	}

	@Test
	public void testListProjectsFiltersByAllowlist() throws Exception {
		ToolDispatcher otherProjectOnly = dispatcher(List.of("other-project"));

		JsonNode result = MAPPER.readTree(otherProjectOnly.dispatch("mesh_list_projects", Map.of()));

		assertThat(result.path("projects")).isEmpty();
	}

	@Test
	public void testListSchemas() throws Exception {
		JsonNode result = dispatch("mesh_list_schemas", Map.of("project", PROJECT_NAME));

		assertThat(result.path("project").asText()).isEqualTo(PROJECT_NAME);
		assertThat(result.path("schemas").findValuesAsText("name")).contains("folder", "content");
	}

	@Test
	public void testGetNode() throws Exception {
		JsonNode result = dispatch("mesh_get_node", Map.of("project", PROJECT_NAME, "uuid", NEWS_UUID, "language", "en"));

		assertThat(result.path("uuid").asText()).isEqualTo(NEWS_UUID);
		assertThat(result.path("project").asText()).isEqualTo(PROJECT_NAME);
		assertThat(result.path("language").asText()).isEqualTo("en");
		assertThat(result.path("schema").asText()).isEqualTo("folder");
	}

	@Test
	public void testGetNodeUnknownUuidIsNotFound() {
		assertThatThrownBy(() -> dispatch("mesh_get_node",
			Map.of("project", PROJECT_NAME, "uuid", "00000000000000000000000000000000", "language", "en")))
			.isInstanceOfSatisfying(AppError.class, e -> assertThat(e.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
	}

	@Test
	public void testGetWebroot() throws Exception {
		String path = call(() -> client().findNodeByUuid(PROJECT_NAME, NEWS_UUID,
			new NodeParametersImpl().setLanguages("en").setResolveLinks(LinkType.SHORT))).getPath();

		JsonNode result = dispatch("mesh_get_webroot", Map.of("project", PROJECT_NAME, "path", path, "language", "en"));

		assertThat(result.path("uuid").asText()).isEqualTo(NEWS_UUID);
		assertThat(result.path("path").asText()).isEqualTo(path);
	}

	@Test
	public void testRunGraphql() throws Exception {
		JsonNode result = dispatch("mesh_run_graphql",
			Map.of("project", PROJECT_NAME, "query", "{ node(uuid: \"" + NEWS_UUID + "\") { uuid } }"));

		assertThat(result.path("data").path("node").path("uuid").asText()).isEqualTo(NEWS_UUID);
	}

	@Test
	public void testRunGraphqlRejectsMutation() {
		assertThatThrownBy(() -> dispatch("mesh_run_graphql",
			Map.of("project", PROJECT_NAME, "query", "mutation { deleteNode(uuid: \"" + NEWS_UUID + "\") }")))
			.isInstanceOfSatisfying(AppError.class, e -> assertThat(e.getCode()).isEqualTo(ErrorCode.FORBIDDEN));
	}

	@Test
	public void testProjectOutsideAllowlistIsForbidden() {
		ToolDispatcher otherProjectOnly = dispatcher(List.of("other-project"));

		assertThatThrownBy(() -> otherProjectOnly.dispatch("mesh_get_node",
			Map.of("project", PROJECT_NAME, "uuid", NEWS_UUID, "language", "en")))
			.isInstanceOfSatisfying(AppError.class, e -> assertThat(e.getCode()).isEqualTo(ErrorCode.FORBIDDEN));
	}

	@Test
	public void testSearchNodes() throws Exception {
		recreateIndices();
		String concordeUuid = tx(() -> content("concorde").getUuid());

		JsonNode result = dispatch("mesh_search_nodes",
			Map.of("project", PROJECT_NAME, "query", "concorde", "language", "en", "publishedOnly", false));

		assertThat(result.path("results").findValuesAsText("uuid")).contains(concordeUuid);
		assertThat(result.path("results").findValuesAsText("language")).containsOnly("en");
	}

	@Test
	public void testSearchNodesPublishedOnlyByDefault() throws Exception {
		recreateIndices();
		String concordeUuid = tx(() -> content("concorde").getUuid());

		// test data content is published, so the default publishedOnly=true still finds it
		JsonNode result = dispatch("mesh_search_nodes",
			Map.of("project", PROJECT_NAME, "query", "concorde", "language", "en"));

		assertThat(result.path("results").findValuesAsText("uuid")).contains(concordeUuid);
	}

	@Test
	public void testSearchNodesPublishedOnlyExcludesDraft() throws Exception {
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setParentNode(new NodeReference().setUuid(NEWS_UUID));
		request.setSchema(new SchemaReferenceImpl().setName("content"));
		request.getFields().put("title", FieldUtil.createStringField("mcpdraftonly"));
		request.getFields().put("teaser", FieldUtil.createStringField("mcpdraftonly"));
		request.getFields().put("slug", FieldUtil.createStringField("mcpdraftonly"));
		// created, but never published
		String draftUuid = call(() -> client().createNode(PROJECT_NAME, request)).getUuid();
		waitForSearchIdleEvent();

		JsonNode publishedOnly = dispatch("mesh_search_nodes",
			Map.of("project", PROJECT_NAME, "query", "mcpdraftonly", "language", "en"));
		JsonNode includingDrafts = dispatch("mesh_search_nodes",
			Map.of("project", PROJECT_NAME, "query", "mcpdraftonly", "language", "en", "publishedOnly", false));

		assertThat(publishedOnly.path("results").findValuesAsText("uuid")).doesNotContain(draftUuid);
		assertThat(includingDrafts.path("results").findValuesAsText("uuid")).contains(draftUuid);
	}

	@Test
	public void testSearchNodesFiltersBySchema() throws Exception {
		recreateIndices();
		String concordeUuid = tx(() -> content("concorde").getUuid());

		JsonNode contentOnly = dispatch("mesh_search_nodes",
			Map.of("project", PROJECT_NAME, "query", "concorde", "language", "en", "schema", "content", "publishedOnly", false));
		JsonNode folderOnly = dispatch("mesh_search_nodes",
			Map.of("project", PROJECT_NAME, "query", "concorde", "language", "en", "schema", "folder", "publishedOnly", false));

		assertThat(contentOnly.path("results").findValuesAsText("uuid")).contains(concordeUuid);
		assertThat(contentOnly.path("results").findValuesAsText("schema")).containsOnly("content");
		assertThat(folderOnly.path("results").findValuesAsText("uuid")).doesNotContain(concordeUuid);
	}

	@Test
	public void testSearchNodesRespectsLimit() throws Exception {
		recreateIndices();

		JsonNode result = dispatch("mesh_search_nodes",
			Map.of("project", PROJECT_NAME, "query", "*", "language", "en", "limit", 1, "publishedOnly", false));

		assertThat(result.path("results")).hasSize(1);
	}

	@Test
	public void testSearchNodesLimitOutOfRangeIsRejected() {
		assertThatThrownBy(() -> dispatch("mesh_search_nodes",
			Map.of("project", PROJECT_NAME, "query", "concorde", "language", "en", "limit", 21)))
			.isInstanceOfSatisfying(AppError.class, e -> assertThat(e.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
	}

	private JsonNode dispatch(String tool, Map<String, Object> args) throws Exception {
		return MAPPER.readTree(dispatcher.dispatch(tool, args));
	}

	private ToolDispatcher dispatcher(List<String> allowedProjects) {
		MeshConfig config = config(username, password, allowedProjects);
		return new ToolDispatcher(config, new MeshClient(config), new MeshCallGuard(config, new Metrics()));
	}

	private MeshConfig config(String username, String password, List<String> allowedProjects) {
		return TestFixtures.stdioConfig("http://localhost:" + getTestContext().getHttpPort(), username, password,
			allowedProjects);
	}
}
