package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.expectResponseMessage;

import org.junit.Test;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, useTinyDataset = true, startServer = true)
public class AdminEndpointTest extends AbstractMeshTest {

	@Test
	public void testMigrationStatusWithNoMigrationRunning() {
		GenericMessageResponse message = call(() -> client().schemaMigrationStatus());
		expectResponseMessage(message, "migration_status_idle");
	}

}
