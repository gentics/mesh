package com.gentics.mesh.core.branch;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.core.rest.common.ObjectPermissionRevokeRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionEndpointTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for branches
 */
@MeshTestSetting(testSize = FULL, startServer = true)
public class BranchRolePermissionsEndpointTest extends AbstractRolePermissionEndpointTest {

	@Override
	protected BaseElement getTestedElement() {
		return project().getInitialBranch();
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		return () -> client().getBranchRolePermissions(PROJECT_NAME, getTestedUuid());
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionGrantRequest request) {
		return () -> client().grantBranchRolePermissions(PROJECT_NAME, getTestedUuid(), request);
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> revokeRolePermissions(ObjectPermissionRevokeRequest request) {
		return () -> client().revokeBranchRolePermissions(PROJECT_NAME, getTestedUuid(), request);
	}
}
