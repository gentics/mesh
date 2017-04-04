package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.expectResponseMessage;

import java.io.IOException;

import static com.gentics.mesh.test.TestSize.PROJECT;

import org.junit.Test;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = true, inMemoryDB = false)
public class AdminEndpointTest extends AbstractMeshTest {

	@Test
	public void testMigrationStatusWithNoMigrationRunning() {
		GenericMessageResponse message = call(() -> client().schemaMigrationStatus());
		expectResponseMessage(message, "migration_status_idle");
	}

	@Test
	public void testBackupRestore() throws IOException {
		try (Tx tx = db().tx()) {
			group().addRole(roles().get("admin"));
			tx.success();
		}
		GenericMessageResponse message = call(() -> client().invokeBackup());
		expectResponseMessage(message, "backup_finished");

		message = call(() -> client().invokeRestore());
		expectResponseMessage(message, "restore_finished");
	}

	@Test
	public void testExportImport() {
		try (Tx tx = db().tx()) {
			group().addRole(roles().get("admin"));
			tx.success();
		}
		GenericMessageResponse message = call(() -> client().invokeExport());
		expectResponseMessage(message, "export_finished");

		message = call(() -> client().invokeImport());
		expectResponseMessage(message, "import_finished");
	}

}
