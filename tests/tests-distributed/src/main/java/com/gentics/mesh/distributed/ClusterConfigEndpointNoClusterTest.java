package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;

import org.junit.Before;
import org.junit.Test;

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
	public void testLoadCoordinationMaster() {
		call(() -> client().loadCoordinationMaster());
	}

	@Test
	public void testSetCoordinationMaster() {
		call(() -> client().setCoordinationMaster(), HttpResponseStatus.BAD_REQUEST, "cluster_coordination_master_set_error_not_electable", options().getNodeName());
	}
}
