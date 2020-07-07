package com.gentics.mesh.distributed;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

/**
 * Same as {@link ClusterCoordinatorTokenTest}, but intended for local testing.
 * This requires a previously started mesh cluster with two nodes "nodeA" and "nodeB".
 * The cluster coordination has to activated and nodeA has to be the master coordinator.
 */
public class ClusterCoordinatorTokenTestLocal {
	private String userName;
	private JWTAuth provider;
	private MeshRestClient serverAClient;
	private MeshRestClient serverBClient;

	@Before
	public void setup() {
		provider = JWTAuth.create(Vertx.vertx(), new JWTAuthOptions()
			.addPubSecKey(new PubSecKeyOptions()
				.setAlgorithm("HS256")
				// public-keys/symmetric-key.json contains the key
				.setPublicKey("test-key")
				.setSymmetric(true)));

		serverAClient = MeshRestClient.create("localhost", 8080, false);
		serverBClient = MeshRestClient.create("localhost", 8081, false);
	 	userName = "testuser" + randomUUID();

	 	assertClusterCoordinatorSetup();
	}

	private void assertClusterCoordinatorSetup() {
		serverBClient.setLogin("admin", "admin");
		serverBClient.login().blockingGet();
		assertThat(serverBClient.loadCoordinationMaster().blockingGet())
			.hasName("nodeA");
	}

	/**
	 * Calls /auth/me with a new token on nodeB which causes a new user to be created.
	 * Asserts that the request has been redirected to nodeA.
	 */
	@Test
	public void createNewUser() {
		serverBClient.setAPIKey(createToken());
		MeshResponse<UserResponse> response = serverBClient.me().getResponse().blockingGet();
		assertThat(response).isForwardedFrom("nodeA");
		assertThat(response.getBody()).hasName(userName);
	}

	@Test
	public void nonMutatingReadRequest() {
		serverBClient.setAPIKey(createToken());

		// This creates the user
		MeshResponse<UserResponse> response = serverBClient.me().getResponse().blockingGet();
		assertThat(response).isForwardedFrom("nodeA");
		assertThat(response.getBody()).hasName(userName);

		// User should not be changed anymore, so no redirect should happen
		response = serverBClient.me().getResponse().blockingGet();
		assertThat(response).isNotForwarded();
		assertThat(response.getBody()).hasName(userName);
	}

	@Test
	public void changedEmail() {
		serverBClient.setAPIKey(createToken());

		// This creates the user
		MeshResponse<UserResponse> response = serverBClient.me().getResponse().blockingGet();
		assertThat(response).isForwardedFrom("nodeA");
		assertThat(response.getBody()).hasName(userName);

		// User should not be changed anymore, so no redirect should happen
		response = serverBClient.me().getResponse().blockingGet();
		assertThat(response).isNotForwarded();
		assertThat(response.getBody()).hasName(userName);

		// Email has changed. The request has to be redirected.
		serverBClient.setAPIKey(createTokenWithEmail("test@gentics.com"));
		response = serverBClient.me().getResponse().blockingGet();
		assertThat(response).isForwardedFrom("nodeA");
		assertThat(response.getBody())
			.hasName(userName)
			.hasEmail("test@gentics.com");
	}

	private String createToken() {
		return provider.generateToken(new JsonObject()
			.put("preferred_username", userName)
			.put("jti", randomUUID())
		);
	}

	private String createTokenWithEmail(String email) {
		return provider.generateToken(new JsonObject()
			.put("preferred_username", userName)
			.put("jti", randomUUID())
			.put("email", email)
		);
	}
}
