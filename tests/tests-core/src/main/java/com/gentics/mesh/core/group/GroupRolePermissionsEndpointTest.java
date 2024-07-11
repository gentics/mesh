package com.gentics.mesh.core.group;

import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;

import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.core.rest.common.ObjectPermissionRevokeRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionEndpointTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for groups
 */
@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true)
public class GroupRolePermissionsEndpointTest extends AbstractRolePermissionEndpointTest {

	@Override
	protected BaseElement getTestedElement() {
		return group();
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		return () -> client().getGroupRolePermissions(getTestedUuid());
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionGrantRequest request) {
		return () -> client().grantGroupRolePermissions(getTestedUuid(), request);
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> revokeRolePermissions(ObjectPermissionRevokeRequest request) {
		return () -> client().revokeGroupRolePermissions(getTestedUuid(), request);
	}
}
