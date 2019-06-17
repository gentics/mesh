package com.gentics.mesh.distributed;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.util.TestUtils.getJson;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.distributed.containers.MeshDockerServer;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.util.TestUtils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Ignore("Fails on CI pipeline. See https://github.com/gentics/mesh/issues/608")
public class BasicClusterTest extends AbstractClusterTest {

	private static String clusterPostFix = randomUUID();

	private static final Logger log = LoggerFactory.getLogger(BasicClusterTest.class);

	// public static MeshLocalServer serverA = new MeshLocalServer("localNodeA", true, true);

	public static MeshDockerServer serverA = new MeshDockerServer(vertx)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withInitCluster()
		.waitForStartup()
		.withClearFolders();

	public static MeshDockerServer serverB = new MeshDockerServer(vertx)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeB")
		.withDataPathPostfix(randomToken())
		.withClearFolders();

	public static MeshRestClient clientA;
	public static MeshRestClient clientB;

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverB).around(serverA);

	@BeforeClass
	public static void waitForNodes() throws InterruptedException {
		LoggingConfigurator.init();
		serverB.awaitStartup(200);
		clientA = serverA.client();
		clientB = serverB.client();
	}

	@Before
	public void setupLogin() {
		clientA.setLogin("admin", "admin");
		clientA.login().blockingGet();
		clientB.setLogin("admin", "admin");
		clientB.login().blockingGet();
	}

	@Test
	public void testClusterStatus() {
		ClusterStatusResponse response = call(() -> clientA.clusterStatus());
		assertThat(response.getInstances()).hasSize(2);
		Set<String> names = response.getInstances().stream().map(i -> i.getName()).collect(Collectors.toSet());
		Set<String> stati = response.getInstances().stream().map(i -> i.getStatus()).collect(Collectors.toSet());
		assertThat(names).containsExactly("nodeA", "nodeB");
		assertThat(stati).containsExactly("ONLINE", "ONLINE");
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
	public void testElementCreation() throws InterruptedException {
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
		Thread.sleep(250);

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
	public void testElementDeletion() throws InterruptedException {
		String projectName = randomName();
		NodeResponse response = createProjectAndNode(clientA, projectName);
		Thread.sleep(250);

		String uuid = response.getUuid();
		call(() -> clientB.deleteNode(projectName, uuid));
		Thread.sleep(250);

		call(() -> clientA.findNodeByUuid(projectName, response.getUuid()), NOT_FOUND, "object_not_found_for_uuid", uuid);
	}

	@Test
	public void testNodeUpdate() {
		String projectName = randomName();
		NodeResponse response = createProjectAndNode(clientA, projectName);

		NodeUpdateRequest request = new NodeUpdateRequest();
		request.setLanguage("de");
		request.getFields().put("teaser", FieldUtil.createStringField("deutscher text"));
		request.getFields().put("slug", FieldUtil.createStringField("new-page.de.html"));
		request.getFields().put("content", FieldUtil.createStringField("Mahlzeit!"));
		NodeResponse updateResponse = call(() -> clientB.updateNode(projectName, response.getUuid(), request, new NodeParametersImpl().setLanguages(
			"de")));
		assertEquals("new-page.de.html", updateResponse.getFields().getStringField("slug").getString());

		NodeResponse responseFromNodeA = call(() -> clientA.findNodeByUuid(projectName, response.getUuid(), new NodeParametersImpl().setLanguages(
			"de")));
		assertEquals("new-page.de.html", responseFromNodeA.getFields().getStringField("slug").getString());

	}

	@Test
	public void testNodeCreation() {
		String projectName = randomName();
		NodeResponse response = createProjectAndNode(clientA, projectName);

		NodeResponse nodeResponse = call(() -> clientB.findNodeByUuid(projectName, response.getUuid()));
		assertEquals("Blessed mealtime again!", nodeResponse.getFields().getStringField("content").getString());
	}

	@Test
	public void testPublishNode() {
		String projectName = randomName();
		NodeResponse response = createProjectAndNode(clientA, projectName);
		String uuid = response.getUuid();

		TestUtils.sleep(1000);

		call(() -> clientB.findNodeByUuid(projectName, uuid));

		BranchListResponse branchesResponse = call(() -> clientA.findBranches(projectName));
		String branchUuid = branchesResponse.getData().get(0).getUuid();

		// NodeA - Assert that the node is offline
		call(() -> clientA.findNodeByUuid(projectName, uuid, new VersioningParametersImpl().published()), NOT_FOUND,
			"node_error_published_not_found_for_uuid_branch_language", uuid, "en", branchUuid);

		call(() -> clientB.findNodeByUuid(projectName, uuid));

		// NodeA - Now publish the node
		call(() -> clientA.publishNode(projectName, uuid));

		// NodeA - Assert that the node can now be loaded
		NodeResponse publishedNode = call(() -> clientA.findNodeByUuid(projectName, uuid, new VersioningParametersImpl().published()));
		assertEquals("en", publishedNode.getLanguage());

		TestUtils.sleep(6000);
		// NodeB - Assert that the node can now be loaded
		publishedNode = call(() -> clientB.findNodeByUuid(projectName, uuid, new VersioningParametersImpl().published()));
		assertEquals("en", publishedNode.getLanguage());
	}

	@Test
	public void testCreateProject() throws InterruptedException {
		String newProjectName = randomName();
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

	@Test
	public void testPermissionChanges() throws InterruptedException {
		// Node A - Create project
		String newProjectName = randomName();
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(newProjectName);
		request.setSchemaRef("folder");
		ProjectResponse projectResponse = call(() -> clientA.createProject(request));

		// Node A - Create role with read perm on project
		RoleResponse roleResponse = call(() -> clientA.createRole(new RoleCreateRequest().setName("newRole")));
		RolePermissionRequest permRequest = new RolePermissionRequest().setRecursive(true);
		permRequest.getPermissions().setRead(true);
		call(() -> clientA.updateRolePermissions(roleResponse.getUuid(), "projects/" + projectResponse.getUuid(), permRequest));

		// Node A - Create Group and user
		GroupResponse groupResponse = call(() -> clientA.createGroup(new GroupCreateRequest().setName("newGroup")));
		call(() -> clientA.addRoleToGroup(groupResponse.getUuid(), roleResponse.getUuid()));
		String username = "dummyUser";
		String password = "dummyPass";
		UserResponse userResponse = call(() -> clientA.createUser(new UserCreateRequest().setUsername(username).setPassword(password)));
		call(() -> clientA.addUserToGroup(groupResponse.getUuid(), userResponse.getUuid()));

		// Node B - Login with new user and read the project
		clientB.logout().blockingGet();
		clientB.setLogin(username, password);
		clientB.login().blockingGet();
		call(() -> clientB.findProjectByUuid(projectResponse.getUuid()));

		// 1. Test permission removal
		permRequest.getPermissions().setRead(false);
		call(() -> clientA.updateRolePermissions(roleResponse.getUuid(), "projects/" + projectResponse.getUuid(), permRequest));
		Thread.sleep(2000);
		call(() -> clientB.findProjectByUuid(projectResponse.getUuid()), FORBIDDEN, "error_missing_perm", projectResponse.getUuid(), READ_PERM.getRestPerm().getName());
		permRequest.getPermissions().setRead(true);
		call(() -> clientA.updateRolePermissions(roleResponse.getUuid(), "projects/" + projectResponse.getUuid(), permRequest));
		Thread.sleep(2000);
		call(() -> clientB.findProjectByUuid(projectResponse.getUuid()));

		// 2. Test role removal - Now remove the role from the group and check that this change is reflected on clientB
		call(() -> clientA.removeRoleFromGroup(groupResponse.getUuid(), roleResponse.getUuid()));
		clientA.logout().blockingGet();
		clientA.setLogin(username, password);
		clientA.login().blockingGet();
		call(() -> clientA.findProjectByUuid(projectResponse.getUuid()), FORBIDDEN, "error_missing_perm", projectResponse.getUuid(), READ_PERM.getRestPerm().getName());
		call(() -> clientB.findProjectByUuid(projectResponse.getUuid()), FORBIDDEN, "error_missing_perm", projectResponse.getUuid(), READ_PERM.getRestPerm().getName());
	}

	/**
	 * Update a project name and assert that the routes are being updated across the cluster.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testProjectUpdate() throws InterruptedException {
		// Node A: Create Project
		String newProjectName = randomName();
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(newProjectName);
		request.setSchemaRef("folder");
		ProjectResponse response = call(() -> clientA.createProject(request));

		// Node A: Update project
		String newName = "newNameForProject";
		call(() -> clientA.updateProject(response.getUuid(), new ProjectUpdateRequest().setName(newName)));

		Thread.sleep(20000);

		// Node B: Only the root node should be found and routes should have been updated across the cluster
		assertThat(call(() -> clientB.findNodes(newName)).getData()).hasSize(1);

	}

	/**
	 * Invoke a schema update and verify that the schema migration is being executed. Validate that the search index and graph was updated across the cluster.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testSchemaUpdate() throws InterruptedException {
		String projectName = randomName();
		NodeResponse response = createProjectAndNode(clientB, projectName);

		// NodeA: Update the schema
		SchemaListResponse schemaListResponse = call(() -> clientB.findSchemas());
		SchemaResponse schemaResponse = schemaListResponse.getData().stream().filter(sr -> sr.getName().equals("content")).findAny().get();
		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setName("content");
		request.setDescription("New description");
		request.setDisplayField("slug");
		request.setSegmentField("slug");
		request.setFields(schemaResponse.getFields());
		request.getFields().add(FieldUtil.createStringFieldSchema("someText"));
		request.validate();
		call(() -> clientA.updateSchema(schemaResponse.getUuid(), request));

		for (int i = 0; i < 10; i++) {
			JobListResponse statusResponse = call(() -> clientA.findJobs());
			if (statusResponse.getData().size() > 0) {
				JobResponse first = statusResponse.getData().get(0);
				if (JobStatus.COMPLETED.equals(first.getStatus())) {
					log.info("Migration completed...");
					break;
				}
			}
			Thread.sleep(1000);
		}

		// Check status on nodeA
		JobListResponse status = call(() -> clientA.findJobs());
		assertEquals(JobStatus.COMPLETED, status.getData().get(0).getStatus());

		// Check status on nodeB
		status = call(() -> clientB.findJobs());
		assertEquals(JobStatus.COMPLETED, status.getData().get(0).getStatus());

		// NodeB: Now verify that the migration on nodeA has updated the node
		NodeResponse response2 = call(() -> clientB.findNodeByUuid(projectName, response.getUuid()));
		assertThat(response.getVersion()).doesNotMatch(response2.getVersion());
		assertThat(response.getSchema().getVersion()).doesNotMatch(response2.getSchema().getVersion());

	}

	@Test
	@Ignore("Elasticsearch will not be started. Thus no searching is supported.")
	public void testNodeSearch() throws IOException, InterruptedException {
		String projectName = randomName();
		NodeResponse response = createProjectAndNode(clientA, projectName);
		String json = getJson("basic-query.json");
		NodeListResponse searchResponse = call(() -> clientB.searchNodes(projectName, json));
		assertThat(searchResponse.getData()).hasSize(1);
		assertEquals(response.getUuid(), searchResponse.getData().get(0).getUuid());
	}

	/**
	 * Verify that the project is deleted and the routes are removed on the other instances.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testProjectDeletion() throws InterruptedException {
		String newProjectName = randomName();
		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(newProjectName);
		request.setSchemaRef("folder");
		ProjectResponse response = call(() -> clientA.createProject(request));
		String uuid = response.getUuid();
		Thread.sleep(250);

		// Node B: Delete the project
		call(() -> clientB.deleteProject(uuid));
		Thread.sleep(250);

		// Node A: Assert that the project can't be found
		call(() -> clientA.findProjectByUuid(uuid), NOT_FOUND, "object_not_found_for_uuid", uuid);
	}

}
