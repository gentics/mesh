package com.gentics.mesh.search;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.google.common.collect.Sets;
import io.vertx.core.AbstractVerticle;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

/**
 * Created by philippguertler on 05.09.16.
 */
public class PermissionSearchVerticleTest extends AbstractSearchVerticleTest {

	private static String QUERY_MATCH_ALL = "{\n" +
			"  \"query\": {\n" +
			"     \"match_all\": { }\n" +
			"  }\n" +
			"}";

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.searchVerticle());
		list.add(meshDagger.groupVerticle());
		list.add(meshDagger.userVerticle());
		list.add(meshDagger.roleVerticle());
		list.add(meshDagger.nodeVerticle());
		list.add(meshDagger.projectVerticle());
		return list;
	}

	private MeshRestClient createTestData() {
		UserCreateRequest req = new UserCreateRequest();
		GroupResponse group = createGroup("restrictedGroup");
		ProjectResponse project = call(() -> getClient().findProjects()).getData().get(0);

		req.setUsername("restrictedUser").setPassword("test1234");
		req.setGroupUuid(group.getUuid());
		UserResponse user = call(() -> getClient().createUser(req));
		RoleResponse role = createRole("restrictedRole", group.getUuid());
		call(() -> getClient().addRoleToGroup(group.getUuid(), role.getUuid()));

		int nodeCount = 500;
		int batchSize = 100;

		NodeResponse lastNode = null;
		int i = 0;
		long timestamp = System.currentTimeMillis();
		NodeCreateRequest nqr = new NodeCreateRequest();
		nqr.setParentNodeUuid(project.getRootNodeUuid());
		nqr.setLanguage("en");
		nqr.setSchema(new SchemaReference().setName("folder"));
		while (i < nodeCount) {
			for (int j = 0; j < batchSize && i + j < nodeCount; j++) {
				nqr.getFields().put("name", new StringFieldImpl().setString("searchNode" + (i+j)));
				lastNode = call(() -> getClient().createNode(PROJECT_NAME, nqr));
			}
			i += batchSize;
			long newStamp = System.currentTimeMillis();
			long diff = newStamp - timestamp;
			System.out.println(String.format("Created %d nodes. Took %.3f seconds", i, diff/1000f));
			timestamp = newStamp;
		}

		// Grant permission on last node
		latchFor(getClient().updateRolePermissions(role.getUuid(), String.format("projects/%s/nodes/%s", project.getUuid(), lastNode.getUuid()), new RolePermissionRequest().setPermissions(Sets.newHashSet("read"))).invoke());

		MeshRestClient restrictedClient = MeshRestClient.create("localhost", getPort(), vertx,
				Mesh.mesh().getOptions().getAuthenticationOptions().getAuthenticationMethod());
		restrictedClient.setLogin(user.getUsername(), "test1234");
		restrictedClient.login().toBlocking().value();

		return restrictedClient;
	}

	private void runQuery(MeshRestClient client, String query, int runs) {
		long timestamp;
		long totalDiff = 0;
		long longestDiff = 0;
		long shortestDiff = Long.MAX_VALUE;
		for (int j = 0; j < runs; j++) {
			timestamp = System.currentTimeMillis();
			NodeListResponse resultList = call(() -> client.searchNodes(query, new VersioningParameters().setVersion("draft")));
			long newStamp = System.currentTimeMillis();
			long diff = newStamp - timestamp;
			if (diff > longestDiff) longestDiff = diff;
			if (diff < shortestDiff) shortestDiff = diff;
			totalDiff += diff;
			assertEquals(1, resultList.getData().size());
		}
		System.out.println(String.format("Search complete. Shortest took %dms. Longest took %dms, avg: %.3fms", shortestDiff, longestDiff, totalDiff / (float)runs));

	}

	private void createRoles(int count) {
		RoleCreateRequest req = new RoleCreateRequest();
		MeshRestClient client = getClient();
		for (int i = 0; i < count; i++) {
			req.setName("testRole" + i);
			RoleResponse role = call(() -> client.createRole(req));
			call(() -> client.updateRolePermissions(role.getUuid(), String.format("projects/%s/nodes", PROJECT_NAME), createPermissionRequest("read")));
		}
	}

	private RolePermissionRequest createPermissionRequest(String... permissions) {
		return new RolePermissionRequest().setPermissions(Sets.newHashSet(permissions));
	}

	@Test
	public void testPermissionPerformanceNoScript() throws Exception {
		MeshRestClient client = createTestData();
		runQuery(client, QUERY_MATCH_ALL, 200);
	}

	@Test
	public void testPermissionPerformanceNoScriptManyRoles() throws Exception {
		createRoles(100);
		MeshRestClient client = createTestData();
		runQuery(client, QUERY_MATCH_ALL, 200);
	}

	@Test
	public void testPermissionPerformanceEmptyScript() throws Exception {
		MeshRestClient client = createTestData();
		String query = "{\n" +
				"  \"query\": {\n" +
				"     \"match_all\": { }\n" +
				"  },\n" +
				"  \"script_fields\": {\n" +
				"    \"meshscript.hasPermission\": {\n" +
				"        \"script\": \"empty\",\n" +
				"        \"lang\": \"native\"\n" +
				"    }\n" +
				"  }\n" +
				"}";
		runQuery(client, query, 200);
	}
}
