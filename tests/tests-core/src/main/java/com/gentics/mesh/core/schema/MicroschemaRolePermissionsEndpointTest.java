package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.core.rest.common.ObjectPermissionRevokeRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionEndpointTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for microschemas
 */
@MeshTestSetting(testSize = FULL, startServer = true)
public class MicroschemaRolePermissionsEndpointTest extends AbstractRolePermissionEndpointTest {

	@Override
	protected BaseElement getTestedElement() {
		return microschemaContainer("vcard");
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		return () -> client().getMicroschemaRolePermissions(getTestedUuid());
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionGrantRequest request) {
		return () -> client().grantMicroschemaRolePermissions(getTestedUuid(), request);
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> revokeRolePermissions(ObjectPermissionRevokeRequest request) {
		return () -> client().revokeMicroschemaRolePermissions(getTestedUuid(), request);
	}
}
