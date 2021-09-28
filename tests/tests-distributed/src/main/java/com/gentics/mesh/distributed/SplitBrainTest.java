package com.gentics.mesh.distributed;

import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import com.gentics.mesh.test.category.ClusterTests;
import com.gentics.mesh.test.docker.MeshContainer;
import com.google.common.collect.Lists;

@Ignore
@Category(ClusterTests.class)
public class SplitBrainTest {

	private static final int STARTUP_TIMEOUT = 500;

	private static String clusterPostFix = randomUUID();

	private static final int WRITE_QUORUM = 2;

	public static MeshContainer serverA = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withInitCluster()
		.withWriteQuorum(WRITE_QUORUM)
		.waitForStartup()
		.withFilesystem()
		.withClearFolders();

	public static MeshContainer serverB = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeB")
		.withDataPathPostfix(randomToken())
		.withFilesystem()
		.withWriteQuorum(WRITE_QUORUM)
		.withClearFolders();

	public static MeshContainer serverC = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeC")
		.withDataPathPostfix(randomToken())
		.withFilesystem()
		.withWriteQuorum(WRITE_QUORUM)
		.withClearFolders();

	public static MeshContainer serverD = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeD")
		.withDataPathPostfix(randomToken())
		.withFilesystem()
		.withWriteQuorum(WRITE_QUORUM)
		.withClearFolders();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverD).around(serverC).around(serverB).around(serverA);

	@BeforeClass
	public static void login() throws InterruptedException {
		serverB.awaitStartup(STARTUP_TIMEOUT);
		serverB.login();
		serverC.awaitStartup(STARTUP_TIMEOUT);
		serverC.login();
		serverD.awaitStartup(STARTUP_TIMEOUT);
		serverD.login();
		serverA.awaitStartup(STARTUP_TIMEOUT);
		serverA.login();
	}

	@Test
	public void testCluster() throws InterruptedException, IOException {
		System.err.println("Ready to split the brain");
		System.in.read();
		invokeSplitBrain();
		System.err.println("Brain split. Press any key to re-merge");
		System.in.read();
		mergeSplitBrain();
		System.out.println("Press any key to terminate test");
		System.in.read();
	}

	private void invokeSplitBrain() {
		List<MeshContainer> runningServers = Arrays.asList(serverA, serverB, serverC, serverD);

		System.err.println("Invoking split brain situation on cluster");
		if (runningServers.size() % 2 == 0) {
			List<List<MeshContainer>> lists = Lists.partition(runningServers, (runningServers.size() + 1) / 2);
			List<MeshContainer> halfA = lists.get(0);
			List<MeshContainer> halfB = lists.get(1);

			// Drop Traffic in halfA to halfB
			for (MeshContainer server : halfA) {
				try {
					server.dropTraffic(halfB.toArray(new MeshContainer[halfB.size()]));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// Drop Traffic in halfB to halfA
			for (MeshContainer server : halfB) {
				try {
					server.dropTraffic(halfA.toArray(new MeshContainer[halfA.size()]));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void mergeSplitBrain() {
		List<MeshContainer> runningServers = Arrays.asList(serverA, serverB, serverC, serverD);
		System.err.println("Merging split brain in cluster");
		for (MeshContainer server : runningServers) {
			try {
				server.resumeTraffic();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
