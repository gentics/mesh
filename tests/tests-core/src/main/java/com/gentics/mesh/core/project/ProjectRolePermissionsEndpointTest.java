package com.gentics.mesh.core.project;

import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for projects
 */
@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true)
public class ProjectRolePermissionsEndpointTest extends AbstractRolePermissionTest {

	@Override
	protected HibBaseElement getTestedElement() {
		return project();
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		return () -> client().getProjectRolePermissions(getTestedUuid());
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionRequest request) {
		// TODO Auto-generated method stub
		return null;
	}
}
