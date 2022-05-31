package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.Test;

import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class OrientDBAdminEndpointTest extends AdminEndpointTest {


	/**
	 * Test clearing the internal caches.
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
