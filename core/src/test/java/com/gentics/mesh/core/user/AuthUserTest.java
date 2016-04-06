package com.gentics.mesh.core.user;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.AbstractBasicDBTest;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.relationship.GraphPermission;

import io.vertx.ext.web.RoutingContext;

public class AuthUserTest extends AbstractBasicDBTest {

	@Test
	public void testAuthorization() throws Exception {
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		MeshAuthUser requestUser = ac.getUser();
		Language targetNode = english();
		assertTrue(requestUser.hasPermissionAsync(ac, targetNode, GraphPermission.READ_PERM).toBlocking().first());

		role().revokePermissions(targetNode, GraphPermission.READ_PERM);
		ac.data().clear();
		assertFalse(requestUser.hasPermissionAsync(ac, targetNode, GraphPermission.READ_PERM).toBlocking().first());
	}

}
