package com.gentics.mesh.distributed.coordinatortoken;

import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.ClassRule;
import org.junit.rules.RuleChain;

import com.gentics.mesh.distributed.containers.MeshDockerServer;
import com.gentics.mesh.rest.client.MeshRestClient;

public class ClusterCoordinatorTokenTest extends AbstractClusterCoordinatorTokenTest {
	private static String clusterPostFix = randomUUID();
	private static String coordinatorRegex = "nodeA";

	public static MeshDockerServer serverA = new MeshDockerServer()
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withCoordinatorPlane()
		.withCoordinatorRegex(coordinatorRegex)
		.withInitCluster()
		.withPlugin(new File("../core/target/test-plugins/basic/target/auth-plugin-0.0.1-SNAPSHOT.jar"), "auth.jar")
		.withPublicKeys(getResourceAsFile("/public-keys/symmetric-key.json"))
		.waitForStartup()
		.withClearFolders();

	public static MeshDockerServer serverB = new MeshDockerServer()
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeB")
		.withDataPathPostfix(randomToken())
		.withCoordinatorPlane()
		.withCoordinatorRegex(coordinatorRegex)
		.withPlugin(new File("../core/target/test-plugins/basic/target/auth-plugin-0.0.1-SNAPSHOT.jar"), "auth.jar")
		.withPublicKeys(getResourceAsFile("/public-keys/symmetric-key.json"))
		.waitForStartup()
		.withClearFolders();

	private static File getResourceAsFile(String name) {
		try {
			return new File(com.gentics.mesh.distributed.ClusterCoordinatorTokenTest.class.getResource(name).toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverA).around(serverB);

	@Override
	protected MeshRestClient getServerBClient() {
		return serverB.client();
	}
}
