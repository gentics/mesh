package com.gentics.mesh.core.tag;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.core.rest.common.ObjectPermissionRevokeRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionEndpointTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for tags
 */
@MeshTestSetting(testSize = FULL, startServer = true)
public class TagRolePermissionsEndpointTest extends AbstractRolePermissionEndpointTest {

	@Override
	protected HibBaseElement getTestedElement() {
		return tag("red");
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		String tagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		String uuid = getTestedUuid();
		return () -> client().getTagRolePermissions(PROJECT_NAME, tagFamilyUuid, uuid);
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionGrantRequest request) {
		String tagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		String uuid = getTestedUuid();
		return () -> client().grantTagRolePermissions(PROJECT_NAME, tagFamilyUuid, uuid, request);
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> revokeRolePermissions(ObjectPermissionRevokeRequest request) {
		String tagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		String uuid = getTestedUuid();
		return () -> client().revokeTagRolePermissions(PROJECT_NAME, tagFamilyUuid, uuid, request);
	}
}
