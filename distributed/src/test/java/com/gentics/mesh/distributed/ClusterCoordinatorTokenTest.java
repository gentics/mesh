package com.gentics.mesh.distributed;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.distributed.containers.MeshDockerServer;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.util.EventUtils;

import io.reactivex.observers.TestObserver;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

public class ClusterCoordinatorTokenTest {
	private static String clusterPostFix = randomUUID();
	private static String coordinatorRegex = "nodeA";

	private JWTAuth provider;
	private String userName;
	private MeshRestClient serverAClient;
	private MeshRestClient serverBClient;

	public static MeshDockerServer serverA = new MeshDockerServer()
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withCoordinatorPlane()
		.withCoordinatorRegex(coordinatorRegex)
		.withInitCluster()
		.withPublicKeys(getResourceAsFile("/public-keys/symmetric-key.json"))
		.waitForStartup()
		.withClearFolders();

	public static MeshDockerServer serverB = new MeshDockerServer()
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeB")
		.withDataPathPostfix(randomToken())
		.withCoordinatorPlane()
		.withCoordinatorRegex(coordinatorRegex)
		.withPublicKeys(getResourceAsFile("/public-keys/symmetric-key.json"))
		.waitForStartup()
		.withClearFolders();

	private static File getResourceAsFile(String name) {
		try {
			return new File(ClusterCoordinatorTokenTest.class.getResource(name).toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverA).around(serverB);

	@BeforeClass
	public static void beforeClass() throws Exception {

	}

	@Before
	public void setUp() throws Exception {
		provider = JWTAuth.create(Vertx.vertx(), new JWTAuthOptions()
			.addPubSecKey(new PubSecKeyOptions()
				.setAlgorithm("HS256")
				// public-keys/symmetric-key.json contains the base64url encoded key
				.setPublicKey("test-key")
				.setSymmetric(true)));
		serverAClient = serverA.client();
		serverBClient = serverB.client();
		userName = "testuser" + randomUUID();
	}

	@Test
	public void createNewUser() throws Exception {
		serverBClient.setAPIKey(createToken());
		TestObserver<MeshElementEventModelImpl> testObserverA = EventUtils.<MeshElementEventModelImpl>listenForEvent(serverAClient, MeshEvent.USER_CREATED)
			.take(1)
			.test();
		TestObserver<MeshElementEventModelImpl> testObserverB = EventUtils.<MeshElementEventModelImpl>listenForEvent(serverBClient, MeshEvent.USER_CREATED)
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
