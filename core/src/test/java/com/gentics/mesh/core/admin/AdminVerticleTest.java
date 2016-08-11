package com.gentics.mesh.core.admin;

import static com.gentics.mesh.util.MeshAssert.latchFor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.admin.AdminVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class AdminVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private AdminVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testMigrationStatusWithNoMigrationRunning() {
		Future<GenericMessageResponse> statusFuture = getClient().schemaMigrationStatus().invoke();
		latchFor(statusFuture);
		expectResponseMessage(statusFuture, "migration_status_idle");
	}

}
