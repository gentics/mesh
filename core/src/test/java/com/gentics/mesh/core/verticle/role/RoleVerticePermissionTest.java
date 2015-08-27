package com.gentics.mesh.core.verticle.role;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class RoleVerticePermissionTest extends AbstractRestVerticleTest {

	@Autowired
	private RoleVerticle verticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testRevokeAllPermissionFromProject() {

		// Add permission on own role
		try (Trx tx = db.trx()) {
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));
			tx.success();
		}

		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(true);
		Future<GenericMessageResponse> future = getClient().updateRolePermission(role().getUuid(), "projects/" + project().getUuid(), request);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("role_updated_permission", future, role().getName());

		try (Trx tx = db.trx()) {
			assertFalse(role().hasPermission(GraphPermission.READ_PERM, tagFamily("colors")));
		}
	}

	@Test
	public void testAddPermissionToProjectTagFamily() {

		// Add permission on own role
		try (Trx tx = db.trx()) {
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));
			tx.success();
		}

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
		
		try (Trx tx = db.trx()) {
			assertFalse(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));
		}
	}
}
