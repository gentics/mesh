package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.expectResponseMessage;
import static com.gentics.mesh.test.TestSize.PROJECT;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
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
		try (Tx tx = tx()) {
			group().addRole(roles().get("admin"));
			tx.success();
		}
		GenericMessageResponse message = call(() -> client().invokeBackup());
		expectResponseMessage(message, "backup_finished");

		message = call(() -> client().invokeRestore());
		expectResponseMessage(message, "restore_finished");
	}

	@Test
	@Ignore("Endpoint disabled")
	public void testExportImport() {
		try (Tx tx = tx()) {
			group().addRole(roles().get("admin"));
			tx.success();
		}
		GenericMessageResponse message = call(() -> client().invokeExport());
		expectResponseMessage(message, "export_finished");

		message = call(() -> client().invokeImport());
		expectResponseMessage(message, "import_finished");
	}

}
