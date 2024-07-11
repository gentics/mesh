package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
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
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.github.jknack.handlebars.internal.lang3.StringUtils;

/**
 * Test cases which count the number of executed GraphQL request queries.
 */
@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateJmxExposure.class, resetBetweenTests = false)
@RunWith(Parameterized.class)
public class GraphQLNestedQueryCountingTest extends AbstractGraphQLChildrenQueryCountingTest {

	/**
	 * Number of nodes to create
	 */
	public final static int NUM_NODES = 3;

	@Parameter(0)
	public int number;

	@Parameters(name = "{index}: number = {0}")
	public static Collection<Object[]> parameters() throws Exception {
		return Arrays.asList(new Object[] { 1 }, new Object[] { 2 }, new Object[] { 3 }, new Object[] { 4 }, new Object[] { 5 });
	}

	protected void fillChildren(String projectName, String parentNodeUuid, int i, Deque<Integer> stack) {
		// create some additional nodes
		IntStream.range(0, NUM_NODES).forEach(ii -> {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setParentNode(new NodeReference().setUuid(parentNodeUuid));
			nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.getFields().put("slug", new StringFieldImpl().setString(RandomStringUtils.randomAlphabetic(5)));
			NodeResponse node = call(() -> client().createNode(projectName, nodeCreateRequest, new NodeParametersImpl().setLanguages("en")));
			if (DEBUG) {
				System.out.println(i + " / " + ii + " / " + stack.stream().map(Object::toString).collect(Collectors.joining("-")));
			}
			if (i < 5) {
				stack.push(ii);
				fillChildren(projectName, node.getUuid(), i+1, stack);
				stack.pop();
			}
		});
	}

	@Before
	public void setup() {
		if (testContext.needsSetup()) {
			String projectName = tx(() -> projectName());
			String parentNodeUuid = tx(() -> folder("2015").getUuid());

			fillChildren(projectName, parentNodeUuid, 1, new ArrayDeque<>());
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}

	/**
	 * Test getting all nodes of a project
	 */
	@Test
	public void testGetAllNodes() {
		String query = String.format(QUERY_ROOT, folder("2015").getUuid(), CHILDREN_QUERY);
		int i = 0;
		do {
			query = String.format(query, "folder_" + i, "folder", StringUtils.EMPTY, ((i+1) < number) ? CHILDREN_QUERY : CHILDREN_FIELDS );
			i++;
		} while (i < number);
		String finalQuery = query;
		if (DEBUG) {
			System.out.println(finalQuery);
		}
		doTest(() -> client().graphql(projectName(), new GraphQLRequest().setQuery(finalQuery)), 10 + ((number-1) * 3 ));
	}
}
