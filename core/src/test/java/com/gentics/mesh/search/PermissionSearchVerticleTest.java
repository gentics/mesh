package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true)
public class PermissionSearchVerticleTest extends AbstractNodeSearchEndpointTest {

	private ProjectResponse project;
	private UserResponse restrictedUser;

	@Before
	public void fetchTestData() {
		project = call(() -> client().findProjects()).getData().get(0);
	}

	private MeshRestClient createTestData() {
		UserCreateRequest req = new UserCreateRequest();
		GroupResponse group = createGroup("restrictedGroup");

		req.setUsername("restrictedUser").setPassword("test1234");
		req.setGroupUuid(group.getUuid());
		this.restrictedUser = call(() -> client().createUser(req));
		RoleResponse role = createRole("restrictedRole", group.getUuid());
		call(() -> client().addRoleToGroup(group.getUuid(), role.getUuid()));

		int nodeCount = 100;
		int batchSize = 25;

		NodeResponse lastNode = null;
		int i = 0;
		long timestamp = System.currentTimeMillis();
		NodeCreateRequest nqr = new NodeCreateRequest();
		nqr.setParentNodeUuid(project.getRootNode().getUuid());
		nqr.setLanguage("en");
		nqr.setSchema(new SchemaReferenceImpl().setName("folder"));
		while (i < nodeCount) {
			for (int j = 0; j < batchSize && i + j < nodeCount; j++) {
				nqr.getFields().put("name", new StringFieldImpl().setString("searchNode" + (i + j)));
				lastNode = call(() -> client().createNode(PROJECT_NAME, nqr));
			}
			i += batchSize;
			long newStamp = System.currentTimeMillis();
			long diff = newStamp - timestamp;
			System.out.println(String.format("Created %d nodes. Took %.3f seconds", i, diff / 1000f));
			timestamp = newStamp;
		}

		// Grant permission on last node
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(true);
		String nodeUuid = lastNode.getUuid();
		call(() -> client().updateRolePermissions(role.getUuid(), String.format("projects/%s/nodes/%s", project.getUuid(), nodeUuid), request));

		MeshRestClient restrictedClient = MeshRestClient.create("localhost", testContext.getPort(), false, vertx());
		restrictedClient.setLogin(restrictedUser.getUsername(), "test1234");
		restrictedClient.login().toBlocking().value();

		return restrictedClient;
	}

	private void runQuery(MeshRestClient client, String query, int runs) {
		long timestamp;
		long totalDiff = 0;
		long longestDiff = 0;
		long shortestDiff = Long.MAX_VALUE;
		for (int i = 0; i < runs; i++) {
			timestamp = System.currentTimeMillis();
			call(() -> client.searchNodes(query, new VersioningParametersImpl().setVersion("draft")));
			long newStamp = System.currentTimeMillis();
			long diff = newStamp - timestamp;
			if (diff > longestDiff) {
				longestDiff = diff;
			}
			if (diff < shortestDiff) {
				shortestDiff = diff;
			}
			totalDiff += diff;
			// assertEquals(1, resultList.getData().size());
		}
		System.out.println(String.format("%d searches complete. Shortest took %dms. Longest took %dms, avg: %.3fms", runs, shortestDiff, longestDiff,
				totalDiff / (float) runs));

	}

	private void createRoles(int count) {
		RoleCreateRequest req = new RoleCreateRequest();
		MeshRestClient client = client();
		long timestamp = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			req.setName("testRole" + i);
			RoleResponse role = call(() -> client.createRole(req));
			call(() -> client.updateRolePermissions(role.getUuid(), String.format("projects/%s/nodes", project.getUuid()),
					createPermissionRequest()));
		}
		long diff = System.currentTimeMillis() - timestamp;
		System.out.println(String.format("Creating %d roles took %.3f seconds", count, diff / 1000f));
	}

	private RolePermissionRequest createPermissionRequest(String... permissions) {
		RolePermissionRequest request = new RolePermissionRequest();
		request.getPermissions().setRead(true);
		return request;
	}

	private String createPermissionScriptQuery(String userUuid) throws IOException {
		String jsonStr = getESQuery("permissionScriptQuery.es");
		DocumentContext path = JsonPath.parse(jsonStr);
		System.out.println(jsonStr);
		path.set("$['script_fields']['meshscript.hasPermission']['params']['userUuid']", userUuid);
		return path.jsonString();
	}

	@Test
	public void testPermissionPerformanceNoScript() throws Exception {
		MeshRestClient client = createTestData();
		runQuery(client, getQueryAll(), 200);
	}

	@Test
	public void testPermissionPerformanceNoScriptManyRoles() throws Exception {
		createRoles(10);
		MeshRestClient client = createTestData();
		runQuery(client, getQueryAll(), 1000);
	}

	@Test
	public void testPermissionPerformanceEmptyScript() throws Exception {
		MeshRestClient client = createTestData();
		runQuery(client, getEmptyScriptQuery(), 200);
	}

	@Test
	public void testPermissionPerformanceEmptyScriptManyRoles() throws Exception {
		createRoles(10);
		MeshRestClient client = createTestData();
		runQuery(client, getEmptyScriptQuery(), 1000);
	}

	@Test
	public void testPermissionPerformancePermissionScriptManyRoles() throws Exception {
		createRoles(10);
		MeshRestClient client = createTestData();
		runQuery(client, createPermissionScriptQuery(restrictedUser.getUuid()), 1000);
	}

	public String getQueryAll() throws IOException {
		return getESQuery("queryAll.es");
	}

	public String getEmptyScriptQuery() throws IOException {
		return getESQuery("emptyScriptQuery.es");
	}
}