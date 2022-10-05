package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionEndpointTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions for nodes
 */
@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true)
public class NodeRolePermissionsEndpointTest extends AbstractRolePermissionEndpointTest {

	@Override
	protected HibBaseElement getTestedElement() {
		return folder("2015");
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		return () -> client().getNodeRolePermissions(PROJECT_NAME, getTestedUuid());
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionRequest request) {
		return () -> client().grantNodeRolePermissions(PROJECT_NAME, getTestedUuid(), request);
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> revokeRolePermissions(ObjectPermissionRequest request) {
		return () -> client().revokeNodeRolePermissions(PROJECT_NAME, getTestedUuid(), request);
	}
}
