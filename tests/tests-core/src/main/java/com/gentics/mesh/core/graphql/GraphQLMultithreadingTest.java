package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.context.MeshTestHelper.awaitConcurrentRequests;

import org.junit.Test;

import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

/**
 * Test for concurrent "first" GraphQL requests.
 * This addresses some multithreading issues when initializing filters (during execution of first GraphQL requests after the start).
 * The test case should remain the ONLY test in this test class.
 */
@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLMultithreadingTest extends AbstractMeshTest {
	/**
	 * Test concurrent GraphQL Requests
	 */
	@Test
	public void testMultithreadedAccess() {
		int nRequests = 10;
		String projectName = projectName();
		String query = "{me{username}}";

		awaitConcurrentRequests(nRequests, i -> client().graphqlQuery(projectName, query));
	}
}
