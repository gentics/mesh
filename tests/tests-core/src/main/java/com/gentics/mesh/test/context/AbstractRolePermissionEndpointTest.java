package com.gentics.mesh.test.context;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.core.rest.common.ObjectPermissionRevokeRequest;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Abstract test class for role permissions test
 */
public abstract class AbstractRolePermissionEndpointTest extends AbstractMeshTest {
	/**
	 * Test reading role permissions
	 */
	@Test
	public void testReadRolePermissions() {
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
	 */
	@Test
	public void testReadRolePermissionWithoutPermission() {
		revokeReadOnTestedElement();
		String uuid = getTestedUuid();
		call(getRolePermissions(), FORBIDDEN, "error_missing_perm", uuid, READ_PERM.getRestPerm().getName());
	}

	/**
	 * Test reading role permissions without permission on all roles
	 */
	@Test
	public void testReadRolePermissionWithoutPermissionOnRole() {
		boolean hasPublishPermissions = tx(() -> getTestedElement().hasPublishPermissions());
		revokeReadOnRole();

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
	 */
	@Test
	public void testGrantRolePermissionsByUuid() {
		String anonymousUuid = tx(() -> roles().get("anonymous").getUuid());
		RoleReference anonymous = tx(() -> roles().get("anonymous").transformToReference());
		RoleReference testRole = tx(() -> role().transformToReference());

		ObjectPermissionGrantRequest request = new ObjectPermissionGrantRequest();
		request.setRead(Arrays.asList(new RoleReference().setUuid(anonymousUuid)));
		ObjectPermissionResponse response = call(grantRolePermissions(request));
		assertThat(response).as("Response").isNotNull();
		assertThat(response.getRead()).as("Roles with read permission").isNotNull().containsOnly(anonymous, testRole);
		assertThat(response.getCreate()).as("Roles with create permission").isNotNull().containsOnly(testRole);
	}

	/**
	 * Test granting role permissions by name
	 */
	@Test
	public void testGrantRolePermissionsByName() {
		RoleReference anonymous = tx(() -> roles().get("anonymous").transformToReference());
		RoleReference testRole = tx(() -> role().transformToReference());

		ObjectPermissionGrantRequest request = new ObjectPermissionGrantRequest();
		request.setUpdate(Arrays.asList(new RoleReference().setName("anonymous")));
		ObjectPermissionResponse response = call(grantRolePermissions(request));
		assertThat(response).as("Response").isNotNull();
		assertThat(response.getUpdate()).as("Roles with update permission").isNotNull().containsOnly(anonymous, testRole);
		assertThat(response.getDelete()).as("Roles with delete permission").isNotNull().containsOnly(testRole);
	}

	/**
	 * Test granting role permissions by unknown uuid
	 */
	@Test
	public void testGrantUnknownRolePermissionsByUuid() {
		String randomUUID = UUIDUtil.randomUUID();
		ObjectPermissionGrantRequest request = new ObjectPermissionGrantRequest();
		request.setUpdate(Arrays.asList(new RoleReference().setUuid(randomUUID)));
		call(grantRolePermissions(request), NOT_FOUND, "object_not_found_for_uuid", randomUUID);
	}

	/**
	 * Test granting role permissions by unknown name
	 */
	@Test
	public void testGrantUnknownRolePermissionsByName() {
		ObjectPermissionGrantRequest request = new ObjectPermissionGrantRequest();
		request.setDelete(Arrays.asList(new RoleReference().setName("bogus")));
		call(grantRolePermissions(request), NOT_FOUND, "object_not_found_for_name", "bogus");
	}

	/**
	 * Test granting role permissions by neither uuid nor name
	 */
	@Test
	public void testGrantInvalidRolePermissions() {
		ObjectPermissionGrantRequest request = new ObjectPermissionGrantRequest();
		request.setCreate(Arrays.asList(new RoleReference()));
		call(grantRolePermissions(request), BAD_REQUEST, "role_reference_uuid_or_name_missing");
	}

	/**
	 * Test granting roles permissions exclusively
	 */
	@Test
	public void testGrantRolePermissionsExclusive() {
		String anonymousUuid = tx(() -> roles().get("anonymous").getUuid());
		RoleReference anonymous = tx(() -> roles().get("anonymous").transformToReference());
		RoleReference testRole = tx(() -> role().transformToReference());

		tx(tx -> {
			HibRole adminObj = roles().get("admin");
			HibRole testRoleObj = role();

			// revoke the permission on the admin role
			tx.roleDao().revokePermissions(testRoleObj, adminObj, READ_PERM);

			// grant some permissions to the admin role
			tx.roleDao().grantPermissions(adminObj, getTestedElement(), UPDATE_PERM, CREATE_PERM, READ_PERM);
		});

		ObjectPermissionGrantRequest request = new ObjectPermissionGrantRequest();
		request.setCreate(Arrays.asList(new RoleReference().setUuid(anonymousUuid)));
		request.setDelete(Arrays.asList(new RoleReference().setUuid(anonymousUuid)));
		request.setExclusive(true);
		ObjectPermissionResponse response = call(grantRolePermissions(request));
		assertThat(response).as("Response").isNotNull();
		assertThat(response.getRead()).as("Roles with read permission").isNotNull().containsOnly(testRole);
		assertThat(response.getUpdate()).as("Roles with update permission").isNotNull().containsOnly(testRole);
		assertThat(response.getCreate()).as("Roles with create permission").isNotNull().containsOnly(anonymous);
		assertThat(response.getDelete()).as("Roles with delete permission").isNotNull().containsOnly(anonymous);

		// check that admin permissions were not changed
		Set<InternalPermission> adminPermissions = tx(tx -> {
			return tx.roleDao().getPermissions(roles().get("admin"), getTestedElement());
		});
		assertThat(adminPermissions).as("Permissions for role admin").isNotNull().containsOnly(UPDATE_PERM, CREATE_PERM, READ_PERM);
	}

	/**
	 * Test granting roles permissions exclusively while ignoring roles
	 */
	@Test
	public void testGrantRolePermissionsExclusiveWithIgnore() {
		String anonymousUuid = tx(() -> roles().get("anonymous").getUuid());
		RoleReference anonymous = tx(() -> roles().get("anonymous").transformToReference());
		RoleReference testRole = tx(() -> role().transformToReference());

		tx(tx -> {
			HibRole adminObj = roles().get("admin");
			HibRole testRoleObj = role();

			// revoke the permission on the admin role
			tx.roleDao().revokePermissions(testRoleObj, adminObj, READ_PERM);

			// grant some permissions to the admin role
			tx.roleDao().grantPermissions(adminObj, getTestedElement(), UPDATE_PERM, CREATE_PERM, READ_PERM);
		});

		ObjectPermissionGrantRequest request = new ObjectPermissionGrantRequest();
		request.setCreate(Arrays.asList(new RoleReference().setUuid(anonymousUuid)));
		request.setDelete(Arrays.asList(new RoleReference().setUuid(anonymousUuid)));
		request.setExclusive(true);
		request.setIgnore(Arrays.asList(testRole));
		ObjectPermissionResponse response = call(grantRolePermissions(request));
		assertThat(response).as("Response").isNotNull();
		assertThat(response.getRead()).as("Roles with read permission").isNotNull().containsOnly(testRole);
		assertThat(response.getUpdate()).as("Roles with update permission").isNotNull().containsOnly(testRole);
		assertThat(response.getCreate()).as("Roles with create permission").isNotNull().containsOnly(anonymous, testRole);
		assertThat(response.getDelete()).as("Roles with delete permission").isNotNull().containsOnly(anonymous, testRole);

		// check that admin permissions were not changed
		Set<InternalPermission> adminPermissions = tx(tx -> {
			return tx.roleDao().getPermissions(roles().get("admin"), getTestedElement());
		});
		assertThat(adminPermissions).as("Permissions for role admin").isNotNull().containsOnly(UPDATE_PERM, CREATE_PERM, READ_PERM);
	}

	/**
	 * Test granting role without permission on the entity
	 */
	@Test
	public void testGrantRoleWithoutPermission() {
		String uuid = getTestedUuid();
		revokeReadOnTestedElement();
		ObjectPermissionGrantRequest request = new ObjectPermissionGrantRequest();
		call(grantRolePermissions(request), FORBIDDEN, "error_missing_perm", uuid, READ_PERM.getRestPerm().getName());
	}

	/**
	 * Test granting role without read permission on the role
	 */
	@Test
	public void testGrantRoleWithoutReadPermissionOnRole() {
		String testRoleUuid = tx(() -> role().getUuid());
		RoleReference testRoleRef = tx(() -> role().transformToReference());
		revokeReadOnRole();
		ObjectPermissionGrantRequest request = new ObjectPermissionGrantRequest();
		request.setCreate(Arrays.asList(testRoleRef));
		call(grantRolePermissions(request), NOT_FOUND, "object_not_found_for_uuid", testRoleUuid);
	}

	/**
	 * Test granting role without update permission on the role
	 */
	@Test
	public void testGrantRoleWithoutUpdatePermissionOnRole() {
		String testRoleUuid = tx(() -> role().getUuid());
		RoleReference testRoleRef = tx(() -> role().transformToReference());
		revokeUpdateOnRole();
		ObjectPermissionGrantRequest request = new ObjectPermissionGrantRequest();
		request.setCreate(Arrays.asList(testRoleRef));
		call(grantRolePermissions(request), FORBIDDEN, "error_missing_perm", testRoleUuid, UPDATE_PERM.getRestPerm().getName());
	}

	/**
	 * Test revoking permissions by uuid
	 */
	@Test
	public void testRevokeRolePermissionsByUuid() {
		String testRoleUuid = tx(() -> role().getUuid());
		RoleReference testRole = tx(() -> role().transformToReference());

		ObjectPermissionRevokeRequest request = new ObjectPermissionRevokeRequest();
		request.setCreate(Arrays.asList(new RoleReference().setUuid(testRoleUuid)));
		ObjectPermissionResponse response = call(revokeRolePermissions(request));
		assertThat(response).as("Response").isNotNull();
		assertThat(response.getCreate()).as("Roles with create permission").isNotNull().isEmpty();
		assertThat(response.getRead()).as("Roles with read permission").isNotNull().containsOnly(testRole);
	}

	/**
	 * Test revoking role permissions by name
	 */
	@Test
	public void testRevokeRolePermissionsByName() {
		String testRoleName = tx(() -> role().getName());
		RoleReference testRole = tx(() -> role().transformToReference());

		ObjectPermissionRevokeRequest request = new ObjectPermissionRevokeRequest();
		request.setUpdate(Arrays.asList(new RoleReference().setName(testRoleName)));
		ObjectPermissionResponse response = call(revokeRolePermissions(request));
		assertThat(response).as("Response").isNotNull();
		assertThat(response.getUpdate()).as("Roles with update permission").isNotNull().isEmpty();
		assertThat(response.getDelete()).as("Roles with delete permission").isNotNull().containsOnly(testRole);
	}

	/**
	 * Test revoking role permissions by unknown uuid
	 */
	@Test
	public void testRevokeUnknownRolePermissionsByUuid() {
		String randomUUID = UUIDUtil.randomUUID();
		ObjectPermissionRevokeRequest request = new ObjectPermissionRevokeRequest();
		request.setUpdate(Arrays.asList(new RoleReference().setUuid(randomUUID)));
		call(revokeRolePermissions(request), NOT_FOUND, "object_not_found_for_uuid", randomUUID);
	}

	/**
	 * Test revoking role permissions by unknown name
	 */
	@Test
	public void testRevoketUnknownRolePermissionsByName() {
		ObjectPermissionRevokeRequest request = new ObjectPermissionRevokeRequest();
		request.setDelete(Arrays.asList(new RoleReference().setName("bogus")));
		call(revokeRolePermissions(request), NOT_FOUND, "object_not_found_for_name", "bogus");
	}

	/**
	 * Test revoking role permissions by neither uuid nor name
	 */
	@Test
	public void testRevokeInvalidRolePermissions() {
		ObjectPermissionRevokeRequest request = new ObjectPermissionRevokeRequest();
		request.setCreate(Arrays.asList(new RoleReference()));
		call(revokeRolePermissions(request), BAD_REQUEST, "role_reference_uuid_or_name_missing");
	}

	/**
	 * Test revoking role without permission on the entity
	 */
	@Test
	public void testRevokeRoleWithoutPermission() {
		String uuid = getTestedUuid();
		revokeReadOnTestedElement();
		ObjectPermissionRevokeRequest request = new ObjectPermissionRevokeRequest();
		call(revokeRolePermissions(request), FORBIDDEN, "error_missing_perm", uuid, READ_PERM.getRestPerm().getName());
	}

	/**
	 * Test revoking role without read permission on the role
	 */
	@Test
	public void testRevokeRoleWithoutReadPermissionOnRole() {
		String testRoleUuid = tx(() -> role().getUuid());
		RoleReference testRoleRef = tx(() -> role().transformToReference());
		revokeReadOnRole();
		ObjectPermissionRevokeRequest request = new ObjectPermissionRevokeRequest();
		request.setCreate(Arrays.asList(testRoleRef));
		call(revokeRolePermissions(request), NOT_FOUND, "object_not_found_for_uuid", testRoleUuid);
	}

	/**
	 * Test revoking role without update permission on the role
	 */
	@Test
	public void testRevokeRoleWithoutUpdatePermissionOnRole() {
		String testRoleUuid = tx(() -> role().getUuid());
		RoleReference testRoleRef = tx(() -> role().transformToReference());
		revokeUpdateOnRole();
		ObjectPermissionRevokeRequest request = new ObjectPermissionRevokeRequest();
		request.setCreate(Arrays.asList(testRoleRef));
		call(revokeRolePermissions(request), FORBIDDEN, "error_missing_perm", testRoleUuid, UPDATE_PERM.getRestPerm().getName());
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
	 * Revoke the read permission on the tested element
	 */
	protected void revokeReadOnTestedElement() {
		tx(tx -> {
			tx.roleDao().revokePermissions(role(), getTestedElement(), READ_PERM);
		});
	}

	/**
	 * Revoke the read permission on the role
	 */
	protected void revokeReadOnRole() {
		tx(tx -> {
			tx.roleDao().revokePermissions(role(), role(), READ_PERM);
		});
	}

	/**
	 * Revoke the update permission on the role
	 */
	protected void revokeUpdateOnRole() {
		tx(tx -> {
			tx.roleDao().revokePermissions(role(), role(), UPDATE_PERM);
		});
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
	protected abstract ClientHandler<ObjectPermissionResponse> grantRolePermissions(ObjectPermissionGrantRequest request);

	/**
	 * Get a client handler that revokes the role permissions from the tested element
	 * @param request request
	 * @return client handler
	 */
	protected abstract ClientHandler<ObjectPermissionResponse> revokeRolePermissions(ObjectPermissionRevokeRequest request);
}
