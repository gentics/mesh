package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionEndpointTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for tag families
 */
@MeshTestSetting(testSize = FULL, startServer = true)
public class TagFamilyRolePermissionsEndpointTest extends AbstractRolePermissionEndpointTest {

	@Override
	protected HibBaseElement getTestedElement() {
		return tagFamily("colors");
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		return () -> client().getTagFamilyRolePermissions(PROJECT_NAME, getTestedUuid());
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionRequest request) {
		return () -> client().grantTagFamilyRolePermissions(PROJECT_NAME, getTestedUuid(), request);
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> revokeRolePermissions(ObjectPermissionRequest request) {
		return () -> client().revokeTagFamilyRolePermissions(PROJECT_NAME, getTestedUuid(), request);
	}
}
