package com.gentics.mesh.distributed.coordinatortoken;

import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static com.gentics.mesh.distributed.AbstractClusterTest.createDefaultMeshContainer;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.ClassRule;
import org.junit.rules.RuleChain;

import com.gentics.mesh.etc.config.cluster.CoordinatorMode;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.docker.MeshContainer;

public class ClusterCoordinatorTokenTest extends AbstractClusterCoordinatorTokenTest {
	private static String clusterPostFix = randomUUID();
	private static String coordinatorRegex = "nodeA";

	public static MeshContainer serverA = createDefaultMeshContainer()
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withCoordinatorPlane(CoordinatorMode.CUD)
		.withCoordinatorRegex(coordinatorRegex)
		.withInitCluster()
		.withPlugin(new File("../../core/target/test-plugins/auth/target/auth-plugin-0.0.1-SNAPSHOT.jar"), "auth.jar")
		.withPublicKeys(getResourceAsFile("/public-keys/symmetric-key.json"))
		.waitForStartup()
		.withClearFolders();

	public static MeshContainer serverB = createDefaultMeshContainer()
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeB")
		.withDataPathPostfix(randomToken())
		.withCoordinatorPlane(CoordinatorMode.CUD)
		.withCoordinatorRegex(coordinatorRegex)
		.withPlugin(new File("../../core/target/test-plugins/auth/target/auth-plugin-0.0.1-SNAPSHOT.jar"), "auth.jar")
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
