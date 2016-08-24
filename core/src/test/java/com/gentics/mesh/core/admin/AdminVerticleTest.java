package com.gentics.mesh.core.admin;

import static com.gentics.mesh.util.MeshAssert.latchFor;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.admin.AdminVerticle;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.AbstractVerticle;

public class AdminVerticleTest extends AbstractIsolatedRestVerticleTest {

	private AdminVerticle verticle;

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testMigrationStatusWithNoMigrationRunning() {
		MeshResponse<GenericMessageResponse> statusFuture = getClient().schemaMigrationStatus().invoke();
		latchFor(statusFuture);
		expectResponseMessage(statusFuture, "migration_status_idle");
	}

}
