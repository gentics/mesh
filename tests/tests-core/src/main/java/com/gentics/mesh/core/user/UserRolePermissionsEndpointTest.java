package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionEndpointTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for users
 */
@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true)
public class UserRolePermissionsEndpointTest extends AbstractRolePermissionEndpointTest {

	@Override
	protected HibBaseElement getTestedElement() {
		return user();
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		return () -> client().getUserRolePermissions(getTestedUuid());
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionRequest request) {
		return () -> client().grantUserRolePermissions(getTestedUuid(), request);
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> revokeRolePermissions(ObjectPermissionRequest request) {
		return () -> client().revokeUserRolePermissions(getTestedUuid(), request);
	}
}
