package com.gentics.mesh.test.context;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.rest.common.ObjectPermissionRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Abstract test class for role permissions test
 */
public abstract class AbstractRolePermissionEndpointTest extends AbstractMeshTest {
	/**
	 * Test reading role permissions
	 * @throws Exception
	 */
	@Test
	public void testReadRolePermissions() throws Exception {
		boolean hasPublishPermissions = tx(() -> getTestedElement().hasPublishPermissions());
		RoleReference testRole = tx(() -> role().transformToReference());

		ObjectPermissionResponse response = call(getRolePermissions());
		assertThat(response).as("Response").isNotNull();
		assertThat(response.getCreate()).as("Roles with create permission").containsOnly(testRole);
		assertThat(response.getDelete()).as("Roles with delete permission").containsOnly(testRole);
		assertThat(response.getRead()).as("Roles with read permission").containsOnly(testRole);
		assertThat(response.getUpdate()).as("Roles with update permission").containsOnly(testRole);
		if (hasPublishPermissions) {
			assertThat(response.getPublish()).as("Roles with publish permission").containsOnly(testRole);
			assertThat(response.getReadPublished()).as("Roles with readPublished permission").containsOnly(testRole);
		} else {
			assertThat(response.getPublish()).as("Roles with publish permission").isNull();
			assertThat(response.getReadPublished()).as("Roles with readPublished permission").isNull();
		}
	}

	/**
	 * Test reading role permissions without permission on the object itself
	 * @throws Exception
	 */
	@Test
	public void testReadRolePermissionWithoutPermission() throws Exception {
		String uuid = tx(() -> getTestedElement().getUuid());
		tx(tx -> {
			tx.roleDao().revokePermissions(role(), getTestedElement(), READ_PERM);
		});
		call(getRolePermissions(), FORBIDDEN, "error_missing_perm", uuid, READ_PERM.getRestPerm().getName());
	}

	/**
	 * Test reading role permissions without permission on all roles
	 * @throws Exception
	 */
	@Test
	public void testReadRolePermissionWithoutPermissionOnRole() throws Exception {
		boolean hasPublishPermissions = tx(() -> getTestedElement().hasPublishPermissions());
		tx(tx -> {
			tx.roleDao().revokePermissions(role(), role(), READ_PERM);
		});

		ObjectPermissionResponse response = call(getRolePermissions());
		assertThat(response).as("Response").isNotNull();
		assertThat(response.getCreate()).as("Roles with create permission").isNotNull().isEmpty();
		assertThat(response.getDelete()).as("Roles with delete permission").isNotNull().isEmpty();
		assertThat(response.getRead()).as("Roles with read permission").isNotNull().isEmpty();
		assertThat(response.getUpdate()).as("Roles with update permission").isNotNull().isEmpty();
		if (hasPublishPermissions) {
			assertThat(response.getPublish()).as("Roles with publish permission").isNotNull().isEmpty();;
			assertThat(response.getReadPublished()).as("Roles with readPublished permission").isNotNull().isEmpty();
		} else {
			assertThat(response.getPublish()).as("Roles with publish permission").isNull();
			assertThat(response.getReadPublished()).as("Roles with readPublished permission").isNull();
		}
	}

	/**
	 * Test granting role permissions by uuid
	 * @throws Exception
	 */
	@Test
	public void testGrantRolePermissionsByUuid() throws Exception {
		String anonymousUuid = tx(() -> roles().get("anonymous").getUuid());
		RoleReference anonymous = tx(() -> roles().get("anonymous").transformToReference());
		RoleReference testRole = tx(() -> role().transformToReference());

		ObjectPermissionRequest request = new ObjectPermissionRequest();
		request.set(new RoleReference().setUuid(anonymousUuid), READ_PERM.getRestPerm(), true);
		ObjectPermissionResponse response = call(grantRolePermissions(request));
		assertThat(response).as("Response").isNotNull();
		assertThat(response.getRead()).as("Roles with read permission").isNotNull().containsOnly(anonymous, testRole);
		assertThat(response.getCreate()).as("Roles with create permission").isNotNull().containsOnly(testRole);
	}

	/**
	 * Test granting role permissions by name
	 * @throws Exception
	 */
	@Test
	public void testGrantRolePermissionsByName() throws Exception {
		RoleReference anonymous = tx(() -> roles().get("anonymous").transformToReference());
		RoleReference testRole = tx(() -> role().transformToReference());

		ObjectPermissionRequest request = new ObjectPermissionRequest();
		request.set(new RoleReference().setName("anonymous"), UPDATE_PERM.getRestPerm(), true);
		ObjectPermissionResponse response = call(grantRolePermissions(request));
		assertThat(response).as("Response").isNotNull();
		assertThat(response.getUpdate()).as("Roles with update permission").isNotNull().containsOnly(anonymous, testRole);
		assertThat(response.getDelete()).as("Roles with delete permission").isNotNull().containsOnly(testRole);
	}

	/**
	 * Test granting role permissions by unknown uuid
	 * @throws Exception
	 */
	@Test
	public void testGrantUnknownRolePermissionsByUuid() throws Exception {
		String randomUUID = UUIDUtil.randomUUID();
		ObjectPermissionRequest request = new ObjectPermissionRequest();
		request.set(new RoleReference().setUuid(randomUUID), UPDATE_PERM.getRestPerm(), true);
		call(grantRolePermissions(request), NOT_FOUND, "object_not_found_for_uuid", randomUUID);
	}

	/**
	 * Test granting role permissions by unknown name
	 * @throws Exception
	 */
	@Test
	public void testGrantUnknownRolePermissionsByName() throws Exception {
		ObjectPermissionRequest request = new ObjectPermissionRequest();
		request.set(new RoleReference().setName("bogus"), DELETE_PERM.getRestPerm(), true);
		call(grantRolePermissions(request), NOT_FOUND, "object_not_found_for_name", "bogus");
	}

	/**
	 * Test granting role permissions by neither uuid nor name
	 * @throws Exception
	 */
	@Test
	public void testGrantInvalidRolePermissions() throws Exception {
		ObjectPermissionRequest request = new ObjectPermissionRequest();
		request.set(new RoleReference(), CREATE_PERM.getRestPerm(), true);
		call(grantRolePermissions(request), BAD_REQUEST, "role_reference_uuid_or_name_missing");
	}

	/**
	 * Get the tested element (this method assumes a running transaction)
	 * @return tested element
	 */
	protected abstract HibBaseElement getTestedElement();

	/**
	 * Get the uuid of the tested element
	 * @return uuid
	 */
	protected String getTestedUuid() {
		return tx(() -> getTestedElement().getUuid());
	}

	/**
	 * Get a client handler that gets the role permissions on the tested element
	 * @return client handler
	 */
	protected abstract ClientHandler<ObjectPermissionResponse> getRolePermissions();

	/**
	 * Get a client handler that grants the role permissions on the tested element
	 * @param request request
	 * @return client handler
	 */
	protected abstract ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionRequest request);
}
