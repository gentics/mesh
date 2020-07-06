package com.gentics.mesh.distributed;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.util.TestUtils;

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

	@Test
	public void createNewUser() throws Exception {
		serverBClient.setAPIKey(createToken());
		TestObserver<MeshElementEventModelImpl> testObserverA = TestUtils.<MeshElementEventModelImpl>listenForEvent(serverAClient, MeshEvent.USER_CREATED)
			.take(1)
			.test();
		TestObserver<MeshElementEventModelImpl> testObserverB = TestUtils.<MeshElementEventModelImpl>listenForEvent(serverBClient, MeshEvent.USER_CREATED)
			.filter(ev -> ev.getOrigin().equals("nodeB"))
			.take(1)
			.test();

		UserResponse userResponse = serverBClient.me().blockingGet();
		assertThat(userResponse).hasName(userName);

		testObserverA.await(200, TimeUnit.MILLISECONDS);
		testObserverA.assertValue(ev -> ev.getOrigin().equals("nodeA") && ev.getName().equals(userName));

		testObserverB.await(200, TimeUnit.MILLISECONDS);
		testObserverB.assertNoValues();
	}

	private String createToken() {
		return provider.generateToken(new JsonObject()
			.put("preferred_username", userName)
		);
	}
}
