package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for users
 */
@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true)
public class UserRolePermissionsEndpointTest extends AbstractRolePermissionTest {

	@Override
	protected HibBaseElement getTestedElement() {
		return user();
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		String uuid = getTestedUuid();
		return () -> client().getUserRolePermissions(uuid);
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionRequest request) {
		String uuid = getTestedUuid();
		return () -> client().grantUserRolePermissions(uuid, request);
	}
}
