package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;

/**
 * Test cases which count the number of executed GraphQL request queries.
 */
@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
@RunWith(Parameterized.class)
public class GraphQLParallelTransactionsEndpointTest extends AbstractGraphQLChildrenQueryCountingTest {

	/**
	 * Number of nodes to create
	 */
	public final static int NUM_NODES = 100;

	@Parameter(0)
	public int number;

	@Parameters(name = "{index}: number = {0}")
	public static Collection<Object[]> parameters() throws Exception {
		return Arrays.asList(new Object[] { 1 }, new Object[] { 2 }, new Object[] { 5 }, new Object[] { 10 }, new Object[] { 50 }, new Object[] { 100 }, new Object[] { 105 });
	}

	protected void fillChildren(String projectName, String parentNodeUuid) {
		// create some additional nodes
		IntStream.range(0, NUM_NODES).forEach(ii -> {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setParentNode(new NodeReference().setUuid(parentNodeUuid));
			nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.getFields().put("slug", new StringFieldImpl().setString(RandomStringUtils.randomAlphabetic(5)));
			call(() -> client().createNode(projectName, nodeCreateRequest, new NodeParametersImpl().setLanguages("en")));
		});
	}

	@Before
	public void setup() {
		if (testContext.needsSetup()) {
			String projectName = tx(() -> projectName());
			String parentNodeUuid = tx(() -> folder("2015").getUuid());

			fillChildren(projectName, parentNodeUuid);
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}

	/**
	 * Test getting all nodes of a project
	 */
	@Test
	public void testGetAllNodes() {
		String query = String.format(QUERY_ROOT, folder("2015").getUuid(), "%s");
		String content = IntStream.range(0, number).mapToObj(i -> String.format(CHILDREN_QUERY, "folder_" + i, "folder", ", page: 1, perPage: " + number, CHILDREN_FIELDS )).collect(Collectors.joining("\n"));
		String finalQuery = String.format(query, content);
		if (DEBUG) {
			System.out.println(finalQuery);
		}
		doTest(() -> client().graphql(projectName(), new GraphQLRequest().setQuery(finalQuery)), 19 + ((number-1) * 3 ));
	}
}
