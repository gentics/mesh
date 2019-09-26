package com.gentics.mesh.auth;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.auth.util.KeycloakUtils;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestContext;

import io.vertx.core.json.JsonObject;
import okhttp3.Request;
import okhttp3.Response;

public abstract class AbstractOAuthTest extends AbstractMeshTest {

	protected void setAdminToken() {
		client().setAPIKey(null);
		client().setLogin("joe1", "test123");
		client().login().blockingGet();
	}

	protected void setClientTokenFromKeycloak() throws IOException {
		JsonObject authInfo = loginKeycloak();
		String token = authInfo.getString("access_token");
		client().setAPIKey(token);
	}

	private JsonObject loginKeycloak() throws IOException {
		String secret = "9b65c378-5b4c-4e25-b5a1-a53a381b5fb4";
		int port = MeshTestContext.getKeycloak().getFirstMappedPort();
		return KeycloakUtils.loginKeycloak("http", "localhost", port, "master-test", "mesh", "dummyuser", "finger", secret);
	}

	protected JsonObject get(String path, String token) throws IOException {
		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.header("Authorization", "Bearer " + token)
			.url("http://localhost:" + testContext.getPort() + path)
			.build();

		Response response = httpClient().newCall(request).execute();
		return new JsonObject(response.body().string());
	}

	protected String get(String path) throws IOException {
		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.url("http://localhost:" + testContext.getPort() + path)
			.build();

		Response response = httpClient().newCall(request).execute();
		System.out.println("Response: " + response.code());
		return response.body().string();
	}

	protected JsonObject loadJson(String path) throws IOException {
		return new JsonObject(IOUtils.toString(getClass().getResource(path), StandardCharsets.UTF_8));
	}

	protected void assertGroupsOfUser(String userName, String... expectedGroupNames) {
		UserResponse user = call(() -> client().me());
		List<String> groupNamesOfUser = user.getGroups().stream().map(g -> g.getName()).collect(Collectors.toList());
		assertThat(groupNamesOfUser).as("Groups of user {" + userName + "}").containsExactlyInAnyOrder(expectedGroupNames);
	}

	protected void assertGroupRoles(String groupName, String... expectedRoles) {
		String groupUuid = tx(() -> boot().groupRoot().findByName(groupName).getUuid());
		RoleListResponse rolesForGroup = call(() -> client().findRolesForGroup(groupUuid));
		List<String> roleNamesOfGroup = rolesForGroup.getData().stream().map(r -> r.getName()).collect(Collectors.toList());
		assertThat(roleNamesOfGroup).as("Roles of group {" + groupName + "}").containsExactlyInAnyOrder(expectedRoles);
	}

}
