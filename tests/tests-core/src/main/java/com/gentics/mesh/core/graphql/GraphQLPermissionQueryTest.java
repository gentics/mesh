package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.jayway.jsonpath.JsonPath;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLPermissionQueryTest extends AbstractMeshTest {

	private final String queryName = "rolePerms";

	@Test
	public void testReadPublishedNodeChildren() throws IOException {
		RoleResponse anonymousRole = client().findRoles().toSingle().blockingGet()
			.getData().stream()
			.filter(role -> role.getName().equals("anonymous"))
			.findAny().get();

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(getGraphQLQuery(queryName));
		request.setVariables(new JsonObject().put("roleUuid", anonymousRole.getUuid()));

		// 3. Invoke the query and assert that the nodes can still be loaded (due to read published)
		GraphQLResponse response = call(() -> client().graphql(PROJECT_NAME, request));
		JsonObject json = new JsonObject(response.toJson());
		System.out.println(json.encodePrettily());
		assertThat(json).compliesToAssertions(queryName);
	}

	/**
	 * Test reading children of a node, when the read permissions come from different roles
	 * @throws IOException
	 */
	@Test
	public void testReadChildrenPermissionsFromDifferentRoles() throws IOException {
		HibProject project = project();
		String baseNodeUuid = tx(() -> project.getBaseNode().getUuid());

		String userUuid = tx(() -> user().getUuid());

		// create node and assign read permission to anonymous role
		NodeCreateRequest create = new NodeCreateRequest();
		create.setParentNode(new NodeReference().setUuid(baseNodeUuid));
		create.setLanguage("en");
		create.setSchemaName("folder");
		create.setGrant(new ObjectPermissionGrantRequest().setExclusive(true).setReadPublished(Arrays.asList(new RoleReference().setName("anonymous"))));
		NodeResponse testNode = call(() -> client().createNode(PROJECT_NAME, create));

		Map<String, String> nodeUuids = new HashMap<>();
		for (String name : Arrays.asList("role_one", "role_two", "role_three")) {
			// create role
			RoleResponse role = call(() -> client().createRole(new RoleCreateRequest().setName(name)));

			// create group
			GroupResponse group = call(() -> client().createGroup(new GroupCreateRequest().setName(name)));

			// add role to group
			call(() -> client().addRoleToGroup(group.getUuid(), role.getUuid()));

			// add user to group
			call(() -> client().addUserToGroup(group.getUuid(), userUuid));

			// create node and set permission
			create.setParentNode(new NodeReference().setUuid(testNode.getUuid()));
			create.setLanguage("en");
			create.setSchemaName("folder");
			create.getFields().put("name", FieldUtil.createStringField(name));
			create.setGrant(new ObjectPermissionGrantRequest().setExclusive(true)
					.setRead(Arrays.asList(new RoleReference().setName(name)))
					.setOthers(true));
			NodeResponse node = call(() -> client().createNode(PROJECT_NAME, create));
			nodeUuids.put(name, node.getUuid());
		}

		// grant update permissions to the "wrong" nodes
		for (Map.Entry<String, String> entry : nodeUuids.entrySet()) {
			String name = entry.getKey();
			String nodeUuid = entry.getValue();
			List<String> granted = new ArrayList<>(Arrays.asList("role_one", "role_two", "role_three"));
			granted.remove(name);
			List<RoleReference> grantedRoles = granted.stream().map(n -> new RoleReference().setName(n)).collect(Collectors.toList());

			call(() -> client().grantNodeRolePermissions(PROJECT_NAME, nodeUuid, new ObjectPermissionGrantRequest().setExclusive(true).setUpdate(grantedRoles)));
		}

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(getGraphQLQuery("childrenPerms"));
		request.setVariables(new JsonObject().put("nodeUuid", testNode.getUuid()));

		GraphQLResponse response = call(() -> client().graphql(PROJECT_NAME, request));
		JsonObject json = new JsonObject(response.toJson());

		assertThat(json).compliesToAssertions("childrenPerms");

		Collection<String> nodeNames = JsonPath.read(json.encodePrettily(), "$.data.node.children.elements[*].fields.name");
		assertThat(nodeNames).as("Returned nodes").containsOnly("role_one", "role_two", "role_three");
	}

}
