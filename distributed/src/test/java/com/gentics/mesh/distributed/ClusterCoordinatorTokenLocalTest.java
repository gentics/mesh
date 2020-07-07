package com.gentics.mesh.distributed;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.util.EventUtils;

import io.reactivex.observers.TestObserver;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

public class ClusterCoordinatorTokenLocalTest {
	private String userName;
	private JWTAuth provider;
	private MeshRestClient serverAClient;
	private MeshRestClient serverBClient;

	@Before
	public void setup() {
		provider = JWTAuth.create(Vertx.vertx(), new JWTAuthOptions()
			.addPubSecKey(new PubSecKeyOptions()
				.setAlgorithm("HS256")
				// public-keys/symmetric-key.json contains the base64url encoded key
				.setPublicKey("test-key")
				.setSymmetric(true)));

		serverAClient = MeshRestClient.create("localhost", 8080, false);
		serverBClient = MeshRestClient.create("localhost", 8081, false);
	 	userName = "testuser" + randomUUID();
	}

	/**
	 * Calls /auth/me with a new token on nodeB which causes a new user to be created.
	 * Asserts that we only receive a user created event from nodeA.
	 */
	@Test
	public void createNewUser() throws Exception {
		assertClusterCoordinatorSetup();

		TestObserver<MeshElementEventModelImpl> testObserverA = EventUtils.userCreated(serverAClient)
			.take(1)
			.test();
		TestObserver<MeshElementEventModelImpl> testObserverB = EventUtils.userCreated(serverBClient)
			.filter(ev -> ev.getOrigin().equals("nodeB"))
			.take(1)
			.test();

		serverBClient.setAPIKey(createToken());
		UserResponse userResponse = serverBClient.me().blockingGet();
		assertThat(userResponse).hasName(userName);

		testObserverA.await(200, TimeUnit.MILLISECONDS);
		testObserverA.assertValue(ev -> ev.getOrigin().equals("nodeA") && ev.getName().equals(userName));

		testObserverB.await(200, TimeUnit.MILLISECONDS);
		testObserverB.assertNoValues();
	}

	private void assertClusterCoordinatorSetup() {
		serverBClient.setLogin("admin", "admin");
		serverBClient.login().blockingGet();
		assertThat(serverBClient.loadCoordinationMaster().blockingGet())
			.hasName("nodeA");
	}

	private String createToken() {
		return provider.generateToken(new JsonObject()
			.put("preferred_username", userName)
		);
	}
}
