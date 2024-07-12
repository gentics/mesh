package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.assertj.MeshAssertions;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigRequest;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorConfig;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Test cases for calling the cluster endpoints on non-clustered instances
 */
@MeshTestSetting(testSize = PROJECT, startServer = true)
public class ClusterConfigEndpointNoClusterTest extends AbstractMeshTest {
	@Before
	public void addAdminPerms() {
		grantAdmin();
	}

	@Test
	public void testLoadClusterConfig() {
		call(() -> client().loadClusterConfig(), HttpResponseStatus.BAD_REQUEST,
				"error_cluster_status_only_available_in_cluster_mode");
	}

	@Test
	public void testUpdateClusterConfig() {
		call(() -> client().updateClusterConfig(new ClusterConfigRequest()), HttpResponseStatus.BAD_REQUEST,
				"error_cluster_status_only_available_in_cluster_mode");
	}

	@Test
	public void testLoadCoordinationMaster() {
		call(() -> client().loadCoordinationMaster());
	}

	@Test
	public void testSetCoordinationMaster() {
		call(() -> client().setCoordinationMaster(), HttpResponseStatus.BAD_REQUEST, "cluster_coordination_master_set_error_not_electable", options().getNodeName());
	}

	@Test
	public void testLoadCoordinationConfig() {
		CoordinatorConfig coordinatorConfig = call(() -> client().loadCoordinationConfig());
		MeshAssertions.assertThat(coordinatorConfig).hasFieldOrPropertyWithValue("mode", CoordinatorMode.DISABLED);
	}

	@Test
	public void testUpdateCoordinationConfig() {
		call(() -> client().updateCoordinationConfig(new CoordinatorConfig()));
	}
}
