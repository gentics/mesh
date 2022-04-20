package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class AdminEndpointTest extends AbstractMeshTest {

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
	 * Test clearing the internal caches
	 */
	@Test
	public void testClearCache() {
		// create project named "project"
		createProject("project");

		// get tag families of project (this will put project into cache)
		call(() -> client().findTagFamilies("project"));
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(1);

		call(() -> client().clearCache(), FORBIDDEN, "error_admin_permission_required");
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(1);
		grantAdmin();

		GenericMessageResponse response = call(() -> client().clearCache());
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(0);
		assertThat(response.getMessage()).as("Response Message").isEqualTo(I18NUtil.get(Locale.ENGLISH, "cache_clear_invoked"));
	}
}
