package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.core.rest.admin.localconfig.LocalConfigModel;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class AdminEndpointTest extends AbstractMeshTest {
	@Before
	public void setup() {
		adminCall(() -> client().updateLocalConfig(new LocalConfigModel().setReadOnly(false)));
	}

	@Test
	public void testMeshStatus() {
		meshApi().setStatus(MeshStatus.WAITING_FOR_CLUSTER);
		MeshStatusResponse status = call(() -> client().meshStatus());
		assertEquals(MeshStatus.WAITING_FOR_CLUSTER, status.getStatus());
		// Normalize the status for the cleanup.
		meshApi().setStatus(MeshStatus.READY);
	}

	@Test
	public void testClusterStatusInNoClusterMode() {
		call(() -> client().clusterStatus(), FORBIDDEN, "error_admin_permission_required");

		grantAdmin();

		call(() -> client().clusterStatus(), BAD_REQUEST, "error_cluster_status_only_available_in_cluster_mode");
	}

	/**
	 * Test that reading the local config is only possible with admin permissions
	 */
	@Test
	public void testReadLocalConfig() {
		call(() -> client().loadLocalConfig(), FORBIDDEN, "error_admin_permission_required");
		grantAdmin();
		LocalConfigModel localConfig = call(() -> client().loadLocalConfig());
		assertThat(localConfig).as("Local config").hasFieldOrPropertyWithValue("readOnly", false);
	}

	/**
	 * Test that writing the local config is only possible with admin permissions
	 */
	@Test
	public void testWriteLocalConfig() {
		LocalConfigModel localConfig = new LocalConfigModel().setReadOnly(true);
		call(() -> client().updateLocalConfig(localConfig), FORBIDDEN, "error_admin_permission_required");
		grantAdmin();
		// the local config should be unchanged
		assertThat(call(() -> client().loadLocalConfig())).as("Local config").hasFieldOrPropertyWithValue("readOnly", false);

		call(() -> client().updateLocalConfig(localConfig));
		assertThat(call(() -> client().loadLocalConfig())).as("Local config").hasFieldOrPropertyWithValue("readOnly", true);
	}
}
