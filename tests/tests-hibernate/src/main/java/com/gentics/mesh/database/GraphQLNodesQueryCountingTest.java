package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;
import com.jayway.jsonpath.JsonPath;

/**
 * Test cases which count the number of executed GraphQL request queries for getting nodes.
 */
@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
public class GraphQLNodesQueryCountingTest extends AbstractCountingTest {
	public final static int NUM_NODES = 50;

	protected final static String NODES_QUERY = "query { nodes %s { elements { uuid %s } } }";

	protected final static String SELECT_SLUG = "... on folder { fields { slug }}";

	protected final static String SCHEMA_FILTER = "(filter: { schema: { is: folder}})";

	protected final static String NAME_FILTER = "(filter: { fields: { folder: {name: {equals: \"bla\"}}}})";

	protected final static String SCHEMA_AND_NAME_FILTER = "(filter: { schema: { is: folder} fields: { folder: {name: {equals: \"bla\"}}}})";

	protected final static String UUIDS_PATH = "$.data.nodes.elements[*].uuid";

	protected final static String SLUGS_PATH = "$.data.nodes.elements[*].fields.slug";

	protected static Set<String> allNodeUuids = new HashSet<>();
	protected static Set<String> allNodeSlugs = new HashSet<>();

	protected static Set<String> blaNodeUuids = new HashSet<>();
	protected static Set<String> blaNodeSlugs = new HashSet<>();

	protected static int ACCEPTABLE_QUERY_COUNT = 15;

	@Before
	public void setup() {
		if (testContext.needsSetup()) {
			String projectName = tx(() -> projectName());
			String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());
			allNodeUuids.add(parentNodeUuid);
			allNodeSlugs.add(null);
			allNodeUuids.add(tx(() -> folder("2015").getUuid()));
			allNodeSlugs.add("2015");
			allNodeUuids.add(tx(() -> folder("news").getUuid()));
			allNodeSlugs.add("News");

			for (int i = 0; i < NUM_NODES; i++) {
				NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
				nodeCreateRequest.setParentNode(new NodeReference().setUuid(parentNodeUuid));
				nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
				nodeCreateRequest.setLanguage("en");
				nodeCreateRequest.getFields().put("name", new StringFieldImpl().setString("bla"));
				nodeCreateRequest.getFields().put("slug", new StringFieldImpl().setString(RandomStringUtils.insecure().nextAlphabetic(5)));
				NodeResponse node = call(() -> client().createNode(projectName, nodeCreateRequest, new NodeParametersImpl().setLanguages("en")));
				allNodeUuids.add(node.getUuid());
				allNodeSlugs.add(node.getFields().getStringField("slug").getString());
				blaNodeUuids.add(node.getUuid());
				blaNodeSlugs.add(node.getFields().getStringField("slug").getString());
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}

	@Test
	public void testAllNodes() {
		String graphQl = NODES_QUERY.formatted("", "");

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(graphQl);

		GraphQLResponse response = doTest(() -> client().graphql(PROJECT_NAME, request), ACCEPTABLE_QUERY_COUNT);
		List<String> uuids = JsonPath.read(response.toJson(), UUIDS_PATH);
		assertThat(uuids).as("Node uuids").hasSameElementsAs(allNodeUuids);
	}

	@Test
	public void testAllNodesWithField() {
		String graphQl = NODES_QUERY.formatted("", SELECT_SLUG);

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(graphQl);

		GraphQLResponse response = doTest(() -> client().graphql(PROJECT_NAME, request), ACCEPTABLE_QUERY_COUNT);
		List<String> uuids = JsonPath.read(response.toJson(), UUIDS_PATH);
		assertThat(uuids).as("Node uuids").hasSameElementsAs(allNodeUuids);
		List<String> slugs = JsonPath.read(response.toJson(), SLUGS_PATH);
		assertThat(slugs).as("Node slugs").hasSameElementsAs(allNodeSlugs);
	}

	@Test
	public void testAllNodesFilteredBySchema() {
		String graphQl = NODES_QUERY.formatted(SCHEMA_FILTER, "");

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(graphQl);

		GraphQLResponse response = doTest(() -> client().graphql(PROJECT_NAME, request), ACCEPTABLE_QUERY_COUNT);
		List<String> uuids = JsonPath.read(response.toJson(), UUIDS_PATH);
		assertThat(uuids).as("Node uuids").hasSameElementsAs(allNodeUuids);
	}

	@Test
	public void testAllNodesFilteredBySchemaWithField() {
		String graphQl = NODES_QUERY.formatted(SCHEMA_FILTER, SELECT_SLUG);

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(graphQl);

		GraphQLResponse response = doTest(() -> client().graphql(PROJECT_NAME, request), ACCEPTABLE_QUERY_COUNT);
		List<String> uuids = JsonPath.read(response.toJson(), UUIDS_PATH);
		assertThat(uuids).as("Node uuids").hasSameElementsAs(allNodeUuids);
		List<String> slugs = JsonPath.read(response.toJson(), SLUGS_PATH);
		assertThat(slugs).as("Node slugs").hasSameElementsAs(allNodeSlugs);
	}

	@Test
	public void testAllNodesFilteredByField() {
		String graphQl = NODES_QUERY.formatted(NAME_FILTER, "");

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(graphQl);

		GraphQLResponse response = doTest(() -> client().graphql(PROJECT_NAME, request), ACCEPTABLE_QUERY_COUNT);
		List<String> uuids = JsonPath.read(response.toJson(), UUIDS_PATH);
		assertThat(uuids).as("Node uuids").hasSameElementsAs(blaNodeUuids);
	}

	@Test
	public void testAllNodesFilteredByFieldWithField() {
		String graphQl = NODES_QUERY.formatted(NAME_FILTER, SELECT_SLUG);

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(graphQl);

		GraphQLResponse response = doTest(() -> client().graphql(PROJECT_NAME, request), ACCEPTABLE_QUERY_COUNT);
		List<String> uuids = JsonPath.read(response.toJson(), UUIDS_PATH);
		assertThat(uuids).as("Node uuids").hasSameElementsAs(blaNodeUuids);
		List<String> slugs = JsonPath.read(response.toJson(), SLUGS_PATH);
		assertThat(slugs).as("Node slugs").hasSameElementsAs(blaNodeSlugs);
	}

	@Test
	public void testAllNodesFilteredByFieldAndSchema() {
		String graphQl = NODES_QUERY.formatted(SCHEMA_AND_NAME_FILTER, "");

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(graphQl);

		GraphQLResponse response = doTest(() -> client().graphql(PROJECT_NAME, request), ACCEPTABLE_QUERY_COUNT);
		List<String> uuids = JsonPath.read(response.toJson(), UUIDS_PATH);
		assertThat(uuids).as("Node uuids").hasSameElementsAs(blaNodeUuids);
	}

	@Test
	public void testAllNodesFilteredByFieldAndSAchemaWithField() {
		String graphQl = NODES_QUERY.formatted(SCHEMA_AND_NAME_FILTER, SELECT_SLUG);

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(graphQl);

		GraphQLResponse response = doTest(() -> client().graphql(PROJECT_NAME, request), ACCEPTABLE_QUERY_COUNT);
		List<String> uuids = JsonPath.read(response.toJson(), UUIDS_PATH);
		assertThat(uuids).as("Node uuids").hasSameElementsAs(blaNodeUuids);
		List<String> slugs = JsonPath.read(response.toJson(), SLUGS_PATH);
		assertThat(slugs).as("Node slugs").hasSameElementsAs(blaNodeSlugs);
	}
}
