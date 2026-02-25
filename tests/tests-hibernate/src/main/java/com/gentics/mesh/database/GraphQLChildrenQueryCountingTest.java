package com.gentics.mesh.database;

import static com.gentics.mesh.test.ClientHelper.call;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

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
 * Test for getting two levels of children with one level having more than 10 children
 */
@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
public class GraphQLChildrenQueryCountingTest extends AbstractGraphQLChildrenQueryCountingTest {
	/**
	 * Number of children, created for the test folder
	 */
	public final static int NUM_CHILDREN = 50;

	@Before
	public void setup() {
		if (testContext.needsSetup()) {
			String projectName = tx(() -> projectName());
			String parentNodeUuid = tx(() -> folder("2015").getUuid());

			for (int i = 0; i < NUM_CHILDREN; i++) {
				NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
				nodeCreateRequest.setParentNode(new NodeReference().setUuid(parentNodeUuid));
				nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("folder"));
				nodeCreateRequest.setLanguage("en");
				nodeCreateRequest.getFields().put("slug", new StringFieldImpl().setString(RandomStringUtils.randomAlphabetic(5)));
				call(() -> client().createNode(projectName, nodeCreateRequest, new NodeParametersImpl().setLanguages("en")));
			}
		}

		// clear the cache
		adminCall(() -> client().clearCache());
	}

	@Test
	public void test() {
		String query = String.format(QUERY_ROOT, folder("2015").getUuid(), CHILDREN_QUERY);
		query = String.format(query, "folder_0", "folder", StringUtils.EMPTY, CHILDREN_QUERY);
		query = String.format(query, "folder_1", "folder", StringUtils.EMPTY, CHILDREN_FIELDS);
		String finalQuery = query;

		// make the query without counting statements, to put everything into the cache, which can be cached
		nonAdminCall(() -> client().graphql(projectName(), new GraphQLRequest().setQuery(finalQuery)));

		// we expect only 10 sql statements, 5 for each level
		doTest(() -> client().graphql(projectName(), new GraphQLRequest().setQuery(finalQuery)), 10);
	}
}
