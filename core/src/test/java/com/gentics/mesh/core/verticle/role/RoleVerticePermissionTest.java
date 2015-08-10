package com.gentics.mesh.core.verticle.role;

import io.vertx.core.Future;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.verticle.RoleVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class RoleVerticePermissionTest extends AbstractRestVerticleTest {

	@Autowired
	private RoleVerticle rolesVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return rolesVerticle;
	}

	@Test
	public void testAddPermissionToProjectTagFamily() {

		// Add permission on own role
		role().grantPermissions(role(), Permission.UPDATE_PERM);
		assertTrue(role().hasPermission(Permission.DELETE_PERM, tagFamily("colors")));
		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(false);
		request.getPermissions().add("read");
		request.getPermissions().add("update");
		request.getPermissions().add("create");
		Future<GenericMessageResponse> future = getClient().updateRolePermission(role().getUuid(),
				"projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid(), request);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("role_updated_permission", future, role().getName());

		assertFalse(role().hasPermission(Permission.DELETE_PERM, tagFamily("colors")));
	}
}
