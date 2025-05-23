package com.gentics.mesh.auth;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.auth.util.KeycloakUtils;
import com.gentics.mesh.core.rest.admin.localconfig.LocalConfigModel;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.UserAPITokenResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.plugin.auth.AuthServicePluginUtils;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.MeshTestContext;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true, useKeycloak = true)
public class OAuth2KeycloakPluginTest extends AbstractOAuthTest {

	@Before
	public void deployPlugin() throws Exception {
		MapperTestPlugin.reset();
		addPublicKey();
		deployPlugin(MapperTestPlugin.class, "myMapper");
	}

	private void addPublicKey() throws Exception {
		String realmName = "master-test";
		int port = MeshTestContext.getKeycloak().getFirstMappedPort();
		Set<JsonObject> keys = KeycloakUtils.loadJWKs("http", "localhost", port, realmName);
		MapperTestPlugin.publicKeys.addAll(keys);
	}

	@Test
	public void testKeycloakAuth() throws Exception {

		// 1. Login the user
		setClientTokenFromKeycloak();

		// 2. Invoke authenticated request
		UserResponse me = call(() -> client().me());
		assertEquals("mapped@email.tld", me.getEmailAddress());
		assertEquals("mappedFirstname", me.getFirstname());
		assertEquals("mappedLastname", me.getLastname());
		assertEquals("dummyuser", me.getUsername());
		String uuid = me.getUuid();

		// 3. Invoke request again to ensure that the previously created user gets returned
		call(() -> client().me());

		UserResponse me2 = call(() -> client().me());

		assertEquals("The uuid should not change. The previously created user should be returned.", uuid, me2.getUuid());
		List<String> groupNames = me2.getGroups().stream().map(GroupReference::getName).collect(Collectors.toList());
		assertThat(groupNames).as("Groups").containsOnly("group1", "group2", "group3");

		assertNotNull(tx(tx -> {
			return tx.groupDao().findByName("group1");
		}));
		assertNotNull(tx(tx -> { return tx.groupDao().findByName("group2"); }));

		assertNotNull(tx(tx -> { return tx.roleDao().findByName("role1"); }));
		assertNotNull(tx(tx -> { return tx.roleDao().findByName("role2"); }));

		// Invoke request without token
		JsonObject meJson = new JsonObject(get(MeshVersion.CURRENT_API_BASE_PATH + "/auth/me"));
		assertEquals("anonymous", meJson.getString("username"));

		setAdminToken();

		// Now invoke request with regular Mesh API token.
		UserAPITokenResponse meshApiToken = call(() -> client().issueAPIToken(me2.getUuid()));
		client().logout().blockingGet();
		client().setAPIKey(meshApiToken.getToken());
		me = call(() -> client().me());
		assertEquals("dummyuser", me.getUsername());

		// Test broken token
		client().setAPIKey("borked");
		call(() -> client().me(), UNAUTHORIZED, "error_not_authorized");

		client().setAPIKey(null);
		UserResponse anonymous = call(() -> client().me());
		assertEquals("anonymous", anonymous.getUsername());
	}

	@Test
	public void testRejectToken() throws IOException {
		MapperTestPlugin.acceptToken = false;
		setClientTokenFromKeycloak();
		call(() -> client().me(), UNAUTHORIZED);
	}

	@Test
	public void testRoleFilter() throws Exception {
		// Test that the role filter will filter role1 of group3
		MapperTestPlugin.roleFilter = (groupName, roleName) -> {
			System.out.println("Filtering {" + groupName + "} / {" + roleName + "}");
			return groupName.equals("group3") && roleName.equals("role1");
		};
		// Invoke request with the token to apply the logic
		setClientTokenFromKeycloak();
		call(() -> client().me());
		assertGroupsOfUser("dummyUser", "group1", "group2", "group3");

		// Check the roles of the group
		setAdminToken();
		assertGroupRoles("group1", "role3");
		assertGroupRoles("group2", "role1");
		assertGroupRoles("group3", "role2"); // role1 should be filtered out

		// Test with no role filter
		MapperTestPlugin.roleFilter = null;
		setClientTokenFromKeycloak();
		call(() -> client().me());
		assertGroupsOfUser("dummyUser", "group1", "group2", "group3");

		setAdminToken();
		assertGroupRoles("group1", "role3");
		assertGroupRoles("group2", "role1");
		assertGroupRoles("group3", "role1", "role2");

		// Add the admin role so that the group1 gets the admin role
		MapperTestPlugin.roleList.add(new RoleResponse().setName("admin").setGroups(new GroupReference().setName("group1")));
		setClientTokenFromKeycloak();
		call(() -> client().me());
		setAdminToken();
		assertGroupRoles("group1", "role3", "admin");

		// Reset the fields and set the filter. This should remove the admin role
		MapperTestPlugin.reset();
		addPublicKey();
		MapperTestPlugin.roleFilter = AuthServicePluginUtils.createRoleFilter(MapperTestPlugin.roleList, MapperTestPlugin.groupList);

		setClientTokenFromKeycloak();
		call(() -> client().me());
		assertGroupsOfUser("dummyUser", "group1", "group2", "group3");
		setAdminToken();
		assertGroupRoles("group1", "role3");
		assertGroupRoles("group2", "role1");
		assertGroupRoles("group3", "role1", "role2");

	}

	@Test
	public void testGroupFilter() throws Exception {
		// Test that the group filter will filter the group1
		MapperTestPlugin.groupFilter = (groupName) -> {
			System.out.println("Filtering {" + groupName + "}");
			return groupName.equals("group1");
		};
		setClientTokenFromKeycloak();
		call(() -> client().me());
		assertGroupsOfUser("dummyUser", "group2", "group3");

		// Test with no group filter
		MapperTestPlugin.groupFilter = null;
		setClientTokenFromKeycloak();
		call(() -> client().me());
		assertGroupsOfUser("dummyUser", "group1", "group2", "group3");

		// Add the admin group and invoke a request to assign the user to the admin group
		MapperTestPlugin.groupList.add(new GroupResponse().setName("admin"));
		setClientTokenFromKeycloak();
		call(() -> client().me());
		assertGroupsOfUser("dummyUser", "group1", "group2", "group3", "admin");

		// Reset and add a group filter. The admin group should now be removed.
		MapperTestPlugin.reset();
		addPublicKey();
		MapperTestPlugin.groupFilter = AuthServicePluginUtils.createGroupFilter(MapperTestPlugin.groupList);
		setClientTokenFromKeycloak();
		call(() -> client().me());
		assertGroupsOfUser("dummyUser", "group1", "group2", "group3");
	}

	@Test
	public void testDefaultUserMapper() throws IOException {
		MapperTestPlugin.userResult = null;
		setClientTokenFromKeycloak();
		UserResponse me = call(() -> client().me());
		assertEquals("dummy@dummy.dummy", me.getEmailAddress());
		assertEquals("Dummy", me.getFirstname());
		assertEquals("User", me.getLastname());
		assertEquals("dummyuser", me.getUsername());
	}

	@Test
	public void testAuthWithMultiplePublicKeys() throws IOException {
		JsonObject jwk = loadJson("/jwk/dummy-jwk.json");
		MapperTestPlugin.publicKeys.add(jwk);
		MapperTestPlugin.userResult = null;
		setClientTokenFromKeycloak();
		UserResponse me = call(() -> client().me());
		assertEquals("dummy@dummy.dummy", me.getEmailAddress());
	}

	@Test
	public void testWebroot() throws IOException {
		// Upload test image
		String parentUuid = tx(() -> folder("2015").getUuid());
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("binary_content");
		nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("MyImage"));
		nodeCreateRequest.setParentNodeUuid(parentUuid);
		NodeResponse createdNode = call(() -> client().createNode(projectName(), nodeCreateRequest));
		uploadImage(createdNode, "en", "binary");

		// 1. Login the user
		setClientTokenFromKeycloak();

		// 1. Invoke request to create groups
		call(() -> client().me());

		// 2. Now grant permissions via admin
		setAdminToken();

		// Apply permissions
		String role1Uuid = tx(tx -> { return tx.roleDao().findByName("role1").getUuid(); });
		RolePermissionRequest updateRequest = new RolePermissionRequest().setRecursive(true);
		updateRequest.getPermissions().setRead(true);
		call(() -> client().updateRolePermissions(role1Uuid, "projects/" + projectUuid(), updateRequest));
		// Assign the role to the group
		String groupUuid = tx(tx -> { return tx.groupDao().findByName("group1").getUuid(); });
		call(() -> client().addRoleToGroup(groupUuid, role1Uuid));

		// Reset the keycloak token
		setClientTokenFromKeycloak();

		String nodePath = "/News/2015";
		MeshWebrootResponse response = call(
			() -> client().webroot(projectName(), nodePath, new NodeParametersImpl().setResolveLinks(LinkType.SHORT)));
		assertEquals(nodePath, response.getNodeResponse().getPath());

		String imagePath = "/News/2015/blume.jpg";
		response = call(() -> client().webroot(projectName(), imagePath));
		assertTrue(response.isBinary());

	}

	@Test
	public void testReadOnlyMode() throws Exception {
		setClientTokenFromKeycloak();
		call(() -> client().me());
		setReadOnly(true);
		// No changes to token
		call(() -> client().me());

		setClientTokenFromKeycloak();
		call(() -> client().me(), METHOD_NOT_ALLOWED, "error_readonly_mode_oauth");
		setReadOnly(false);
		call(() -> client().me());
	}

	private void setReadOnly(boolean readOnly) {
		String key = client().getAPIKey();
		setAdminToken();
		call(() -> client().updateLocalConfig(new LocalConfigModel().setReadOnly(readOnly)));
		client().setAPIKey(key);
	}
}
