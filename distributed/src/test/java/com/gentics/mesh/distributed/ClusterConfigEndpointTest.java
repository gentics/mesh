package com.gentics.mesh.distributed;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static org.junit.Assert.assertEquals;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigResponse;
import com.gentics.mesh.core.rest.admin.cluster.ClusterServerConfig;
import com.gentics.mesh.core.rest.admin.cluster.ServerRole;
import com.gentics.mesh.distributed.containers.MeshDockerServer;

public class ClusterConfigEndpointTest extends AbstractClusterTest {

	private static String clusterPostFix = randomUUID();

	public static MeshDockerServer serverA = new MeshDockerServer()
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withInitCluster()
		.waitForStartup()
		.withClearFolders();

	public static MeshDockerServer serverB = new MeshDockerServer()
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeB")
		.withDataPathPostfix(randomToken())
		.withClearFolders();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverB).around(serverA);

	@Test
	public void testReadClusterConfig() {
		serverA.client().setLogin("admin", "admin");
		serverA.client().login().blockingGet();

		ClusterConfigResponse config = call(() -> serverA.client().loadClusterConfig());
		System.out.println(config.toJson());

		// assertEquals("The read quorum did not match.", 2, config.getReadQuorum().intValue());
		assertEquals("The write quorum did not match.", "majority", config.getWriteQuorum());
		assertThat(config.getServers()).hasSize(2);

		ClusterConfigRequest request = config.toRequest();
		request.setWriteQuorum("2");
		ClusterServerConfig serverConfig = request.getServers().stream()
			.filter(s -> s.getName().equals("nodeB"))
			.findFirst().get();
		serverConfig.setRole(ServerRole.REPLICA);

		ClusterConfigResponse response = call(() -> serverA.client().updateClusterConfig(request));
		assertEquals("The read quorum did not match.", 1, response.getReadQuorum().intValue());
		assertEquals("The write quorum did not match.", "2", response.getWriteQuorum());
		ClusterServerConfig updatedServerConfig = response.getServers().stream()
			.filter(s -> s.getName().equals("nodeB"))
			.findFirst().get();
		assertEquals("The server should be in replica mode now.", ServerRole.REPLICA, updatedServerConfig.getRole());
	}

}
