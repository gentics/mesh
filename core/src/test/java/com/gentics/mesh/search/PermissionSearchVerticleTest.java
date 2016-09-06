package com.gentics.mesh.search;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectResponse;
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
import static com.gentics.mesh.util.MeshAssert.getResult;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;

/**
 * Created by philippguertler on 05.09.16.
 */
public class PermissionSearchVerticleTest extends AbstractSearchVerticleTest {

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

	@Test
	public void testPermissionPerformance() throws Exception {
		// 1. Preperation
		UserCreateRequest req = new UserCreateRequest();
		GroupResponse group = createGroup("restrictedGroup");
		ProjectResponse project = getResult(getClient().findProjects().invoke()).getData().get(0);

		req.setUsername("restrictedUser").setPassword("test1234");
		req.setGroupUuid(group.getUuid());
		UserResponse user = getResult(getClient().createUser(req).invoke());
		RoleResponse role = createRole("restrictedRole", group.getUuid());
		call(() -> getClient().addUserToGroup(group.getUuid(), user.getUuid()));

		int nodeCount = 50;
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
		NodeListResponse res = call(() -> getClient().findNodes(PROJECT_NAME, new VersioningParameters().setVersion("draft")));

		MeshRestClient restrictedClient = MeshRestClient.create("localhost", getPort(), vertx,
				Mesh.mesh().getOptions().getAuthenticationOptions().getAuthenticationMethod());
		restrictedClient.setLogin(user.getUsername(), "test1234");
		restrictedClient.login().toBlocking().value();

		// 2. Execute Test
		String query = "{\n" +
				"  \"query\": {\n" +
				"    \"match_all\": {}\n" +
				"  }\n" +
				"}";
		timestamp = System.currentTimeMillis();
		NodeListResponse resultList = call(() -> restrictedClient.searchNodes(query, new VersioningParameters().setVersion("draft")));
		long newStamp = System.currentTimeMillis();
		long diff = newStamp - timestamp;

		System.out.println(String.format("Search complete. Took %.3f seconds", diff/1000f));
		assertEquals(1, resultList.getData().size());
	}
}
