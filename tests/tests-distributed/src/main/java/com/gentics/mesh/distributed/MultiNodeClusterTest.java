package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.MeshTestContext.MeshTestInstance;

/**
 * Test how a cluster behaves with more than two nodes.
 */
@MeshTestSetting(testSize = FULL, startServer = true, clusterMode = true, clusterName = "MultiNodeClusterTest", clusterInstances = 4)
public class MultiNodeClusterTest extends AbstractMeshClusteringTest {
	protected MeshTestInstance serverA;
	protected MeshTestInstance serverB;
	protected MeshTestInstance serverC;
	protected MeshTestInstance serverD;

	@Before
	public void setup() {
		serverA = getInstance(0);
		serverB = getInstance(1);
		serverC = getInstance(2);
		serverD = getInstance(3);
	}

	/**
	 * Test that a cluster with multiple nodes can form and that changes are distributed.
	 */
	@Test
	public void testCluster() throws InterruptedException {
		ProjectResponse response = call(
			() -> serverA.getHttpClient().createProject(new ProjectCreateRequest().setName(RandomStringUtils.randomAlphabetic(10)).setSchemaRef("folder")));
		Thread.sleep(1000);

		call(() -> serverB.getHttpClient().findProjectByUuid(response.getUuid()));
		call(() -> serverC.getHttpClient().findProjectByUuid(response.getUuid()));
		call(() -> serverD.getHttpClient().findProjectByUuid(response.getUuid()));

		ProjectResponse response2 = call(
			() -> serverD.getHttpClient().createProject(new ProjectCreateRequest().setName(RandomStringUtils.randomAlphabetic(10)).setSchemaRef("folder")));

		Thread.sleep(1000);
		call(() -> serverA.getHttpClient().findProjectByUuid(response2.getUuid()));
		call(() -> serverB.getHttpClient().findProjectByUuid(response2.getUuid()));
		call(() -> serverC.getHttpClient().findProjectByUuid(response2.getUuid()));
		call(() -> serverD.getHttpClient().findProjectByUuid(response2.getUuid()));
	}
}
