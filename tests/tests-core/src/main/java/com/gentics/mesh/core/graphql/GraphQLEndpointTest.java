package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.MeshVersion.CURRENT_API_VERSION;
import static com.gentics.mesh.test.ClientHelper.call;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.assertj.impl.JsonObjectAssert;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointTest extends AbstractGraphQLEndpointTest {

	protected final String queryName;

	protected final boolean withMicroschema;

	private final boolean withBranchPathPrefix;

	protected final String version;
	protected final String apiVersion;

	protected final Consumer<JsonObject> assertion;
	protected MeshRestClient client;

	/**
	 * Default constructor.
	 *
	 * <p>
	 * When <code>assertion</code> is <code>null</code> the result of the GraphQL query is passed to
	 * {@link JsonObjectAssert#compliesToAssertions(String)} which will check the assertions annotated in the
	 * GraphQL query comments.
	 * </p>
	 *
	 * @param queryName The filename of the GraphQL query to use
	 * @param withMicroschema Whether to use micro schemas
	 * @param withBranchPathPrefix whether the branch should have a path prefix set
	 * @param version Whether to use the <code>draft</code> or <code>published</code> version
	 * @param assertion A custom assertion to be applied on the GraphQL query result
	 */
	public GraphQLEndpointTest(String queryName, boolean withMicroschema, boolean withBranchPathPrefix, String version, Consumer<JsonObject> assertion, String apiVersion) {
		this.queryName = queryName;
		this.withMicroschema = withMicroschema;
		this.withBranchPathPrefix = withBranchPathPrefix;
		this.version = version;
		this.assertion = assertion;
		this.apiVersion = apiVersion;
	}

	@Parameters(name = "query={0},version={3},apiVersion={5}")
	public static Collection<Object[]> paramData() {
		return streamQueries()
			.flatMap(testCase -> IntStream.rangeClosed(1, CURRENT_API_VERSION).mapToObj(version -> {
				// Make sure all testData entries have six parts.
				Object[] array = testCase.toArray(new Object[6]);
				array[5] = "v" + version;
				return array;
			})).collect(Collectors.toList());
	}

	@Before
	public void setUp() throws Exception {
		this.client = client(apiVersion);
	}

	@Test
	public void testNodeQuery() throws Exception {
		prepareNodes(client, withMicroschema, withBranchPathPrefix);

		GraphQLResponse response = call(() -> makeTheQuery(client, queryName, apiVersion, version));
		testTheQuery(response, queryName, apiVersion, assertion);
	}
}
