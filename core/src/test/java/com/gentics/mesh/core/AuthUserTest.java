package com.gentics.mesh.core;

import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.test.AbstractDBTest;

import io.vertx.ext.web.RoutingContext;

public class AuthUserTest extends AbstractDBTest {

	protected UserInfo info;

	@Before
	public void setup() throws Exception {
		setupData();
		info = data().getUserInfo();
	}

	@Test
	public void testAuthorization() throws InterruptedException {
		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		Language targetNode = english();
		final CountDownLatch latch = new CountDownLatch(1);

		requestUser.isAuthorised(targetNode, GraphPermission.READ_PERM, rh -> {
			if (rh.failed()) {
				rh.cause().printStackTrace();
				fail(rh.cause().getMessage());
			}
			assertTrue(rh.result());
			latch.countDown();
		});
		failingLatch(latch);

		info.getRole().revokePermissions(targetNode, GraphPermission.READ_PERM);
		final CountDownLatch latch2 = new CountDownLatch(1);
		requestUser.isAuthorised(targetNode, GraphPermission.READ_PERM, rh -> {
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
