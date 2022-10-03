package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for tag families
 */
@MeshTestSetting(testSize = FULL, startServer = true)
public class TagFamilyRolePermissionsEndpointTest extends AbstractRolePermissionTest {

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
		// TODO Auto-generated method stub
		return null;
	}
}
