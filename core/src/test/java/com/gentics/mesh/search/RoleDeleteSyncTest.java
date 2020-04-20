package com.gentics.mesh.search;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestSetting;

/**
 * Reproducer for https://github.com/gentics/mesh/issues/1041
 */
@RunWith(Parameterized.class)
@MeshTestSetting(startServer = true, testSize = TestSize.PROJECT)
public class RoleDeleteSyncTest extends AbstractMultiESTest {
	public RoleDeleteSyncTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testSyncAfterDeletedRole() throws Exception {
		RoleResponse role = createRole("testRole");
		ProjectResponse project = getProject();
		client().updateRolePermissions(
			role.getUuid(),
			"/projects/" + project.getUuid(),
			RolePermissionRequest.grantAll()
		).blockingAwait();
		client().deleteRole(role.getUuid()).blockingAwait();

		recreateIndices();
	}
}
