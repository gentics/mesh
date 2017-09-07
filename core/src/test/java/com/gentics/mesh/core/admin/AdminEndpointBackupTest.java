package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.assertMessage;
import static com.gentics.mesh.test.TestSize.PROJECT;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = true, inMemoryDB = false)
public class AdminEndpointBackupTest extends AbstractMeshTest {

	@Test
	public void testBackupRestore() throws IOException {
		try (Tx tx = tx()) {
			group().addRole(roles().get("admin"));
			tx.success();
		}
		GenericMessageResponse message = call(() -> client().invokeBackup());
		assertMessage(message, "backup_finished");

		message = call(() -> client().invokeRestore());
		assertMessage(message, "restore_finished");
	}

	@Test
	@Ignore("Endpoint disabled")
	public void testExportImport() {
		try (Tx tx = tx()) {
			group().addRole(roles().get("admin"));
			tx.success();
		}
		GenericMessageResponse message = call(() -> client().invokeExport());
		assertMessage(message, "export_finished");

		message = call(() -> client().invokeImport());
		assertMessage(message, "import_finished");
	}

}
