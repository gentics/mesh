package com.gentics.mesh.distributed.coordinatortoken;

import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.ClassRule;
import org.junit.rules.RuleChain;

import com.gentics.mesh.etc.config.cluster.CoordinationTopologyLockHeldStrategy;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.docker.MeshContainer;

public class ClusterTopologyLockedTest extends AbstractClusterCoordinatorTokenTest {
	private static String clusterPostFix = randomUUID();
	private static String coordinatorRegex = "nodeA";

	public static MeshContainer serverA = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withTopologyLockHeldStrategy(CoordinationTopologyLockHeldStrategy.DROP_CUD)
		.withInitCluster()
		.withPublicKeys(getResourceAsFile("/public-keys/symmetric-key.json"))
		.waitForStartup()
		.withClearFolders();

	public static MeshContainer serverB = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeB")
		.withDataPathPostfix(randomToken())
		.withTopologyLockHeldStrategy(CoordinationTopologyLockHeldStrategy.DROP_CUD)
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
