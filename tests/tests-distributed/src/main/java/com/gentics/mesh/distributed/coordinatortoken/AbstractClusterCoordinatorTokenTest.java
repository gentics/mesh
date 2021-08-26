package com.gentics.mesh.distributed.coordinatortoken;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.AbstractMeshRestHttpClient;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;


public abstract class AbstractClusterCoordinatorTokenTest {
	private String userName;
	private JWTAuth provider;
	private AbstractMeshRestHttpClient serverBClient;
	private AbstractMeshRestHttpClient serverBAdminClient;

	protected abstract MeshRestClient getServerBClient();

	@Before
	public void setup() {
		provider = JWTAuth.create(Vertx.vertx(), new JWTAuthOptions()
			.addPubSecKey(new PubSecKeyOptions()
				.setAlgorithm("HS256")
				// public-keys/symmetric-key.json contains the key
				.setPublicKey("test-key")
				.setSymmetric(true)));

		serverBClient = (AbstractMeshRestHttpClient) getServerBClient();
		serverBAdminClient = (AbstractMeshRestHttpClient) MeshRestClient.create(serverBClient.getConfig());
	 	userName = "testuser" + randomUUID();

	 	assertClusterCoordinatorSetup();
	}

	private void assertClusterCoordinatorSetup() {
		serverBAdminClient.setLogin("admin", "admin");
		serverBAdminClient.login().blockingGet();
		assertThat(serverBAdminClient.loadCoordinationMaster().blockingGet())
			.hasName("nodeA");
	}

	/**
	 * Calls /auth/me with a new token on nodeB which causes a new user to be created.
	 * Asserts that the request has been redirected to nodeA.
	 */
	@Test
	public void createNewUser() {
		createInitialUser();
	}

	@Test
	public void nonMutatingReadRequest() {
		createInitialUser();
		checkRepeatedRead();
	}

	@Test
	public void changedEmail() {
		createInitialUser();
		MeshResponse<UserResponse> response;
		checkRepeatedRead();

		// Email has changed. The request has to be redirected.
		serverBClient.setAPIKey(new TokenBuilder().setMail("test@gentics.com").build());
		response = serverBClient.me().getResponse().blockingGet();
		assertThat(response).isForwardedFrom("nodeA");
		assertThat(response.getBody())
			.hasName(userName)
			.hasEmail("test@gentics.com");

		checkRepeatedRead();
	}

	@Test
	public void addGroup() {
		createInitialUser();

		serverBClient.setAPIKey(new TokenBuilder().addGroup("testGroup").build());
		MeshResponse<UserResponse> response = serverBClient.me().getResponse().blockingGet();
		assertThat(response).isForwardedFrom("nodeA");
		assertThat(response.getBody())
			.hasName(userName)
			.hasGroup("testGroup");
		checkRepeatedRead();

		assertThat(serverBAdminClient.findGroups().blockingGet()).contains("testGroup");
	}

	@Test
	public void addRole() {
		createInitialUser();

		serverBClient.setAPIKey(new TokenBuilder()
			.addGroup("testGroup", "testRole")
			.build());
		MeshResponse<UserResponse> response = serverBClient.me().getResponse().blockingGet();
		assertThat(response).isForwardedFrom("nodeA");
		assertThat(response.getBody())
			.hasName(userName);

		checkRepeatedRead();

		assertThat(serverBAdminClient.findGroups().blockingGet())
			.containsGroupWithRoles("testGroup", "testRole");
	}

	private void checkRepeatedRead() {
		// User should not be changed anymore, so no redirect should happen
		MeshResponse<UserResponse> response = serverBClient.me().getResponse().blockingGet();
		assertThat(response).isNotForwarded();
		assertThat(response.getBody()).hasName(userName);
	}

	private void createInitialUser() {
		serverBClient.setAPIKey(new TokenBuilder().build());

		// This creates the user
		MeshResponse<UserResponse> response = serverBClient.me().getResponse().blockingGet();
		assertThat(response).isForwardedFrom("nodeA");
		assertThat(response.getBody()).hasName(userName);
	}

	private class TokenBuilder {
		private final JsonObject token = new JsonObject()
			.put("preferred_username", userName)
			.put("jti", randomUUID());

		public TokenBuilder setMail(String email) {
			token.put("email", email);
			return this;
		}

		public TokenBuilder addGroup(String groupName, String... roles) {
			if (!token.containsKey("groups")) {
				token.put("groups", new JsonArray());
			}
			token.getJsonArray("groups").add(new JsonObject()
				.put("name", groupName)
				.put("roles", new JsonArray(Arrays.asList(roles)))
			);
			return this;
		}

		public String build() {
			return provider.generateToken(token);
		}
	}
}
