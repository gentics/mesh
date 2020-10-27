package com.gentics.mesh.core.admin;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.parameter.client.BackupParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.UUIDUtil;

@MeshTestSetting(elasticsearch = NONE, testSize = FULL, startServer = true, inMemoryDB = false)
public class AdminEndpointBackupLocalTest extends AbstractMeshTest {

	@Test
	public void testBackupInconsistentDB() throws IOException {
		final String backupDir = testContext.getOptions().getStorageOptions().getBackupDirectory();
		assertFilesInDir(backupDir, 0);

		grantAdminRole();
		GenericMessageResponse message = call(() -> client().invokeBackup(new BackupParametersImpl().setConsistencyCheck(true)));
		assertThat(message).matches("backup_finished");

		// Now produce inconsistency
		Node bogusNode = tx(tx -> {
			Node bogus = tx.getGraph().addFramedVertex(NodeImpl.class);
			bogus.setUuid(UUIDUtil.randomUUID());
			bogus.setProject(project());
			return bogus;
		});

		// Re-Run the backup and expect a failure
		call(() -> client().invokeBackup(new BackupParametersImpl().setConsistencyCheck(true)), INTERNAL_SERVER_ERROR,
			"backup_consistency_check_failed", "1");
		assertFilesInDir(backupDir, 1);

		// Remove the node to avoid test consistency check errors
		tx(tx -> {
			bogusNode.remove();
		});
	}

	@Test
	@Ignore("Endpoint disabled")
	public void testExportImport() {
		grantAdminRole();
		GenericMessageResponse message = call(() -> client().invokeExport());
		assertThat(message).matches("export_finished");

		message = call(() -> client().invokeImport());
		assertThat(message).matches("import_finished");
	}

}
