package com.gentics.mesh.core.admin;

import org.junit.Test;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class AdminVerticleTest extends AbstractRestVerticleTest {

	@Test
	public void testMigrationStatusWithNoMigrationRunning() {
		GenericMessageResponse message = call(() -> getClient().schemaMigrationStatus());
		expectResponseMessage(message, "migration_status_idle");
	}

}
