package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.rest.admin.cluster.ClusterInstanceInfo;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true, clusterMode = true)
public class ClusterStatusTest extends AbstractMeshTest {

	@Test
	public void testLoadStatus() {
		grantAdmin();

		ClusterStatusResponse response = call(() -> client().clusterStatus());
		assertThat(response.getInstances()).hasSize(1);
		ClusterInstanceInfo info = response.getInstances().get(0);
		assertNotNull(info.getAddress());
		assertEquals("ONLINE", info.getStatus());
		assertNotNull(info.getStartDate());
		assertEquals(options().getNodeName(), info.getName());
	}

}
