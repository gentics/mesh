package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.distributed.containers.MeshDevServer;
import com.gentics.mesh.rest.client.MeshRestClient;

public class MeshDevServerTest {

	public static MeshDevServer serverA = new MeshDevServer("nodeA", true);

	public static MeshDevServer serverB = new MeshDevServer("nodeB", false);

	public static MeshRestClient clientA;
	public static MeshRestClient clientB;

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverA).around(serverB);

	@BeforeClass
	public static void waitForNodes() throws InterruptedException {
		clientA = serverA.getMeshClient();
		clientB = serverB.getMeshClient();
	}

//	@Test
//	public void testServer() throws InterruptedException {
//
//		// Node A: Load the anonymous user
//		UserListResponse list = call(() -> clientA.findUsers());
//		UserResponse anonymousUser = list.getData().stream().filter(user -> user.getUsername().equals("anonymous")).findFirst().get();
//		assertFalse(list.getData().isEmpty());
//
//		// Node B: Update the anonymous user
//		UserUpdateRequest updateRequest = new UserUpdateRequest();
//		updateRequest.setUsername("changed");
//		call(() -> clientB.updateUser(anonymousUser.getUuid(), updateRequest));
//
//		// Node A: Load the anonymous user
//		UserResponse nodeAResponse = call(() -> clientA.findUserByUuid(anonymousUser.getUuid()));
//		assertEquals("The username was not changed on node A although we changed it on node B", "changed", nodeAResponse.getUsername());
//
//	}

	@Test
	public void testCreateProject() {
		String newProjectName = "clusteredProject";
		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(newProjectName);
		request.setSchemaRef("folder");
		call(() -> clientA.createProject(request));

		// Node B: List nodes of created project - We expect the REST route should work.
//		NodeListResponse response = call(() -> clientB.findNodes(newProjectName));
//		assertTrue(response.getData().isEmpty());
	}
}
