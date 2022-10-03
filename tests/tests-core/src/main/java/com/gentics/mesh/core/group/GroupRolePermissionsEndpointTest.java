package com.gentics.mesh.core.group;

import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for groups
 */
@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true)
public class GroupRolePermissionsEndpointTest extends AbstractRolePermissionTest {

	@Override
	protected HibBaseElement getTestedElement() {
		return group();
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		String uuid = getTestedUuid();
		return () -> client().getGroupRolePermissions(uuid);
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionRequest request) {
		String uuid = getTestedUuid();
		return () -> client().grantGroupRolePermissions(uuid, request);
	}
}
