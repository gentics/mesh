package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class AdminEndpointTest extends AbstractMeshTest {

	@Test
	public void testMeshStatus() {
		Mesh.mesh().setStatus(MeshStatus.WAITING_FOR_CLUSTER);
		MeshStatusResponse status = call(() -> client().meshStatus());
		assertEquals(MeshStatus.WAITING_FOR_CLUSTER, status.getStatus());
	}

	@Test
	public void testClusterStatusInNoClusterMode() {
		call(() -> client().clusterStatus(), FORBIDDEN, "error_admin_permission_required");

		tx(() -> group().addRole(roles().get("admin")));

		call(() -> client().clusterStatus(), BAD_REQUEST, "error_cluster_status_only_aviable_in_cluster_mode");
	}

}
