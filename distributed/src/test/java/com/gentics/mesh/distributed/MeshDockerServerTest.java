package com.gentics.mesh.distributed;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.distributed.containers.MeshDockerServer;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.core.Vertx;

public class MeshDockerServerTest {

	private static Vertx vertx = Vertx.vertx();
	// public static MeshLocalServer serverA = new MeshLocalServer("localNodeA", true, true);

	public static MeshDockerServer serverA = new MeshDockerServer("nodeA", true, true, vertx, 8000);

	public static MeshDockerServer serverB = new MeshDockerServer("nodeB", false, false, vertx, null);

	public static MeshRestClient clientA;
	public static MeshRestClient clientB;

	@ClassRule
	// public static RuleChain chain = RuleChain.outerRule(serverA).around(serverB);
	public static RuleChain chain = RuleChain.outerRule(serverB).around(serverA);

	@BeforeClass
	public static void waitForNodes() throws InterruptedException {
		serverB.awaitStartup(200);
		clientA = serverA.getMeshClient();
		clientB = serverB.getMeshClient();
	}

	@Test
	public void testElementUpdate() throws InterruptedException {

		// Node A: Load the anonymous user
		UserListResponse list = call(() -> clientA.findUsers());
		UserResponse anonymousUser = list.getData().stream().filter(user -> user.getUsername().equals("anonymous")).findFirst().get();
		assertFalse(list.getData().isEmpty());

		// Node B: Update the anonymous user
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setUsername("changed");
		call(() -> clientB.updateUser(anonymousUser.getUuid(), updateRequest));

		// Node A: Load the anonymous user
		UserResponse nodeAResponse = call(() -> clientA.findUserByUuid(anonymousUser.getUuid()));
		assertEquals("The username was not changed on node A although we changed it on node B", "changed", nodeAResponse.getUsername());

		// Node A: Update the anonymous user
		UserUpdateRequest updateRequest2 = new UserUpdateRequest();
		updateRequest2.setUsername("changed2");
		call(() -> clientA.updateUser(anonymousUser.getUuid(), updateRequest2));

		// Node A: Load the anonymous user
		UserResponse response = call(() -> clientA.findUserByUuid(anonymousUser.getUuid()));
		assertEquals("The username was not changed on node A although we changed it on node A", "changed2", response.getUsername());

		// Node B: Load the anonymous user
		response = call(() -> clientB.findUserByUuid(anonymousUser.getUuid()));
		assertEquals("The username was not changed on node B although we changed it on node A", "changed2", response.getUsername());

	}

	@Test
	public void testElementCreation() {
		// Node A: Create user
		UserCreateRequest createRequest = new UserCreateRequest();
		createRequest.setUsername("clusterdUser");
		createRequest.setPassword("pw");
		UserResponse response = call(() -> clientA.createUser(createRequest));

		// Node B: Verify that the user was created
		assertNotNull(call(() -> clientB.findUserByUuid(response.getUuid())));

		// Node B: Update the user
		call(() -> clientB.updateUser(response.getUuid(), new UserUpdateRequest().setUsername("clusteredUserChanged")));

		// Node A: Verify update of user
		assertEquals("clusteredUserChanged", call(() -> clientA.findUserByUuid(response.getUuid())).getUsername());

		// Node B: Create user
		createRequest.setUsername("clusterdUser2");
		UserResponse response2 = call(() -> clientB.createUser(createRequest));

		// Node A: Verify that the user was created
		assertNotNull(call(() -> clientA.findUserByUuid(response2.getUuid())));

		// Node A: Update the user
		call(() -> clientA.updateUser(response2.getUuid(), new UserUpdateRequest().setUsername("clusteredUser2Changed")));

		// Node B: Verify the update
		assertEquals("clusteredUser2Changed", call(() -> clientB.findUserByUuid(response2.getUuid())).getUsername());

	}

	@Test
	public void testElementDeletion() {

	}

	@Test
	public void testNodeCreation() {

		String projectName = "projectForNodeCreation";

		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(projectName);
		request.setSchemaRef("folder");
		ProjectResponse projectResponse = call(() -> clientA.createProject(request));
		String folderUuid = projectResponse.getRootNode().getUuid();

		// Node A: Find the content schema
		SchemaListResponse schemaListResponse = call(() -> clientA.findSchemas());
		String contentSchemaUuid = schemaListResponse.getData().stream().filter(sr -> sr.getName().equals("content")).map(sr -> sr.getUuid())
				.findAny().get();

		// Node A: Assign content schema to project
		call(() -> clientA.assignSchemaToProject(projectName, contentSchemaUuid));

		// Node A: Create node
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName("content");
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some teaser"));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("new-page.html"));
		nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		nodeCreateRequest.setSchema(schemaReference);
		nodeCreateRequest.setParentNodeUuid(folderUuid);

		NodeResponse response = call(() -> clientA.createNode(projectName, nodeCreateRequest));

		NodeResponse nodeResponse = call(() -> clientB.findNodeByUuid(projectName, response.getUuid()));
		assertEquals("Blessed mealtime again!", nodeResponse.getFields().getStringField("content").getString());

	}

	@Test
	public void testNodeUpdate() {

	}

	@Test
	public void testCreateProject() throws InterruptedException {
		String newProjectName = "clusteredProject";
		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(newProjectName);
		request.setSchemaRef("folder");
		call(() -> clientA.createProject(request));

		Thread.sleep(1000);

		// Node A: List nodes of created project - We expect the REST route should work.
		NodeListResponse response = call(() -> clientA.findNodes(newProjectName));
		assertEquals(1, response.getData().size());

		Thread.sleep(1000);

		// Node B: List nodes of created project - We expect the REST route should work.
		response = call(() -> clientB.findNodes(newProjectName));
		assertEquals(1, response.getData().size());

	}

	/**
	 * Update a project name and assert that the routes are being updated across the cluster.
	 */
	@Test
	public void testProjectUpdate() {
		String newProjectName = "clusteredProject1";
		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(newProjectName);
		request.setSchemaRef("folder");
		ProjectResponse response = call(() -> clientA.createProject(request));

		String newName = "newNameForProject";
		call(() -> clientA.updateProject(response.getUuid(), new ProjectUpdateRequest().setName(newName)));

		// Only the root node should be found
		assertThat(call(() -> clientB.findNodes(newName)).getData()).hasSize(1);

	}

	/**
	 * Invoke a schema update and verify that the schema migration is being executed. Validate that the search index and graph was updated across the cluster.
	 */
	@Test
	public void testSchemaUpdate() {

	}

	/**
	 * Verify that the project is deleted.
	 */
	@Test
	public void testProjectDeletion() {
		String newProjectName = "clusteredProject2";
		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(newProjectName);
		request.setSchemaRef("folder");
		ProjectResponse response = call(() -> clientA.createProject(request));
		String uuid = response.getUuid();

		// Node B: Delete the project
		call(() -> clientB.deleteProject(uuid));

		// Node A: Assert that the project can't be found
		call(() -> clientA.findProjectByUuid(uuid), NOT_FOUND, "object_not_found_for_uuid", uuid);
	}
}
