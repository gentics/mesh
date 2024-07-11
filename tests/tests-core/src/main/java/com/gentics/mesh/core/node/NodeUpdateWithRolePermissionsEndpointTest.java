package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.core.rest.common.ObjectPermissionRevokeRequest;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractRolePermissionEndpointTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases for handling role permissions when updating nodes.
 * The test methods for reading/revoking roles are ignored, because when updating, it is only possible to grant roles.
 */
@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true)
public class NodeUpdateWithRolePermissionsEndpointTest extends AbstractRolePermissionEndpointTest  {
	@Override
	protected BaseElement getTestedElement() {
		return folder("2015");
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> getRolePermissions() {
		return () -> client().getNodeRolePermissions(PROJECT_NAME, getTestedUuid());
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionGrantRequest request) {
		return () -> {
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion("draft");
			update.setGrant(request);
			client().updateNode(PROJECT_NAME, getTestedUuid(), update).blockingAwait();
			return client().getNodeRolePermissions(PROJECT_NAME, getTestedUuid());
		};
	}

	@Override
	protected ClientHandler<ObjectPermissionResponse> revokeRolePermissions(ObjectPermissionRevokeRequest request) {
		return () -> client().revokeNodeRolePermissions(PROJECT_NAME, getTestedUuid(), request);
	}

	@Override
	@Ignore
	@Test
	public void testReadRolePermissions() {
	}

	@Override
	@Ignore
	@Test
	public void testReadRolePermissionWithoutPermission() {
	}

	@Override
	@Ignore
	@Test
	public void testReadRolePermissionWithoutPermissionOnRole() {
	}

	@Override
	@Ignore
	@Test
	public void testRevokeInvalidRolePermissions() {
	}

	@Override
	@Ignore
	@Test
	public void testRevokeRolePermissionsByName() {
	}

	@Override
	@Ignore
	@Test
	public void testRevokeRolePermissionsByUuid() {
	}

	@Override
	@Ignore
	@Test
	public void testRevokeRoleWithoutPermission() {
	}

	@Override
	@Ignore
	@Test
	public void testRevokeRoleWithoutReadPermissionOnRole() {
	}

	@Override
	@Ignore
	@Test
	public void testRevokeRoleWithoutUpdatePermissionOnRole() {
	}

	@Override
	@Ignore
	@Test
	public void testRevoketUnknownRolePermissionsByName() {
	}

	@Override
	@Ignore
	@Test
	public void testRevokeUnknownRolePermissionsByUuid() {
	}
}
