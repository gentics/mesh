package com.gentics.mesh.core;

import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.test.AbstractDBTest;

public class MeshShiroUserTest extends AbstractDBTest {

	protected UserInfo info;

	@Before
	public void setup() throws JsonParseException, JsonMappingException, IOException {
		setupData();
		info = data().getUserInfo();
	}

	@Test
	public void testAuthorization() throws InterruptedException {
		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		Language targetNode = data().getEnglish();
		final CountDownLatch latch = new CountDownLatch(1);

		requestUser.isAuthorised(targetNode, Permission.READ_PERM, rh -> {
			if (rh.failed()) {
				rh.cause().printStackTrace();
				fail(rh.cause().getMessage());
			}
			assertTrue(rh.result());
			latch.countDown();
		});
		assertTrue(latch.await(10, TimeUnit.SECONDS));

		info.getRole().revokePermissions(targetNode, Permission.READ_PERM);
		final CountDownLatch latch2 = new CountDownLatch(1);
		requestUser.isAuthorised(targetNode, Permission.READ_PERM, rh -> {
			if (rh.failed()) {
				rh.cause().printStackTrace();
				fail(rh.cause().getMessage());
			}
			assertFalse(rh.result());
			latch2.countDown();
		});
		assertTrue(latch2.await(10, TimeUnit.SECONDS));

	}

	@Test
	public void testAuthentication() {

	}
}
