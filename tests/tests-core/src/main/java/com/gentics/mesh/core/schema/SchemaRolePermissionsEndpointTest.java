package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.core.rest.common.ObjectPermissionRevokeRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionEndpointTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for schemas
 */
@MeshTestSetting(testSize = FULL, startServer = true)
public class SchemaRolePermissionsEndpointTest extends AbstractRolePermissionEndpointTest {

	@Override
	protected HibBaseElement getTestedElement() {
		return schemaContainer("content");
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		return () -> client().getSchemaRolePermissions(getTestedUuid());
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionGrantRequest request) {
		return () -> client().grantSchemaRolePermissions(getTestedUuid(), request);
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> revokeRolePermissions(ObjectPermissionRevokeRequest request) {
		return () -> client().revokeRoleRolePermissions(getTestedUuid(), request);
	}
}
