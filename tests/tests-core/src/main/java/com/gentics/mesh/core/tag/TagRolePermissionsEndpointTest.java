package com.gentics.mesh.core.tag;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for tags
 */
@MeshTestSetting(testSize = FULL, startServer = true)
public class TagRolePermissionsEndpointTest extends AbstractRolePermissionTest {

	@Override
	protected HibBaseElement getTestedElement() {
		return tag("red");
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		String tagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		return () -> client().getTagRolePermissions(PROJECT_NAME, tagFamilyUuid, getTestedUuid());
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionRequest request) {
		// TODO Auto-generated method stub
		return null;
	}
}
