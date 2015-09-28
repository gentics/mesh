package com.gentics.mesh.core.user;

import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.ext.web.RoutingContext;

public class AuthUserTest extends AbstractBasicDBTest {

	@Test
	public void testAuthorization() throws Exception {
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		MeshAuthUser requestUser = ac.getUser();
		Language targetNode = english();
		final CountDownLatch latch = new CountDownLatch(1);
		requestUser.hasPermission(ac, targetNode, GraphPermission.READ_PERM, rh -> {
			if (rh.failed()) {
				rh.cause().printStackTrace();
				fail(rh.cause().getMessage());
			}
			assertTrue(rh.result());
			latch.countDown();
		});
		failingLatch(latch);

		role().revokePermissions(targetNode, GraphPermission.READ_PERM);
		ac.data().clear();
		final CountDownLatch latch2 = new CountDownLatch(1);
		requestUser.hasPermission(ac, targetNode, GraphPermission.READ_PERM, rh -> {
			if (rh.failed()) {
				rh.cause().printStackTrace();
				fail(rh.cause().getMessage());
			}
			assertFalse(rh.result());
			latch2.countDown();
		});
		failingLatch(latch2);

	}

}
