package com.gentics.mesh.distributed.coordinator;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.gentics.mesh.distributed.AbstractMeshClusteringTest;
import com.gentics.mesh.distributed.AwaitMembershipEvent;
import com.gentics.mesh.test.MeshTestSetting;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * Test for the automatic re-election of the coordination master, when the current master leaves the cluster.
 * This case is the only test case in this test class, because it will shut down the hazelcast instance of one of the cluster members.
 */
@MeshTestSetting(testSize = FULL, startServer = true, clusterMode = true, clusterName = "MasterReelectionTest")
public class MasterReelectionTest extends AbstractMeshClusteringTest {
	/**
	 * Test automatic re-election
	 */
	@Test
	public void testMasterElection() throws Exception {
		// make instance 2 the coordination master
		call(() -> getInstance(1).getHttpClient().setCoordinationMaster());

		// other instances should be aware of that
		assertThat(call(() -> getInstance(0).getHttpClient().loadCoordinationMaster()).getName())
				.as("Coordination master according to " + getInstanceName(0)).isEqualTo(getInstanceName(1));
		assertThat(call(() -> getInstance(2).getHttpClient().loadCoordinationMaster()).getName())
				.as("Coordination master according to " + getInstanceName(2)).isEqualTo(getInstanceName(1));

		try (AwaitMembershipEvent awaitCluster = new AwaitMembershipEvent(
				Hazelcast.getHazelcastInstanceByName(getInstanceName(0)).getCluster(), 10, TimeUnit.SECONDS)) {
			// shut down instance 2
			HazelcastInstance hzInstance2 = Hazelcast.getHazelcastInstanceByName(getInstanceName(1));
			hzInstance2.shutdown();
		}

		// get master info from other instances (should not be instance 2 any more)
		String coordinationMasterFromInstance1 = call(() -> getInstance(0).getHttpClient().loadCoordinationMaster())
				.getName();
		String coordinationMasterFromInstance3 = call(() -> getInstance(2).getHttpClient().loadCoordinationMaster())
				.getName();

		assertThat(coordinationMasterFromInstance1).as("Coordination master according to " + getInstanceName(0)).isNotEqualTo(getInstanceName(1))
				.isEqualTo(coordinationMasterFromInstance3);
	}
}
