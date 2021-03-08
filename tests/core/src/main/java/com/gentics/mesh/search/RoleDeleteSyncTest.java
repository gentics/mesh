package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.project.ProjectListResponse;
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
		recreateIndices();

		RoleResponse role = createRole("testRole");
		ProjectResponse project = getProject();
		client().updateRolePermissions(
			role.getUuid(),
			"/projects/" + project.getUuid(),
			RolePermissionRequest.grantAll()
		).blockingAwait();
		client().deleteRole(role.getUuid()).blockingAwait();

		recreateIndices();
		ProjectListResponse response = client().searchProjects(getSimpleQuery("name", PROJECT_NAME)).blockingGet();
		assertThat(response.getData()).isNotEmpty();
	}
}
