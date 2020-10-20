package com.gentics.mesh.core.admin;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_BACKUP_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_BACKUP_START;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_RESTORE_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_RESTORE_START;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.parameter.client.BackupParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.UUIDUtil;

@MeshTestSetting(elasticsearch = NONE, testSize = FULL, startServer = true, inMemoryDB = false)
public class AdminEndpointBackupLocalTest extends AbstractMeshTest {

	@Test
	public void testBackupRestore() throws IOException {
		final String NEW_PROJECT_NAME = "enemenemuh";
		final String backupDir = testContext.getOptions().getStorageOptions().getBackupDirectory();
		assertFilesInDir(backupDir, 0);
		grantAdmin();

		expect(GRAPH_BACKUP_START).one();
		expect(GRAPH_BACKUP_FINISHED).one();
		GenericMessageResponse message = call(() -> client().invokeBackup());
		awaitEvents();

		assertThat(message).matches("backup_finished");
		assertFilesInDir(backupDir, 1);

		// Now create a project which is not in the backup. The routes and data must vanish when inserting the backup
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(NEW_PROJECT_NAME);
		request.setSchemaRef("folder");
		ProjectResponse projectResponse = call(() -> client().createProject(request));
		String baseNodeUuid = projectResponse.getRootNode().getUuid();
		call(() -> client().findNodeByUuid(NEW_PROJECT_NAME, baseNodeUuid));

		expect(GRAPH_RESTORE_START).one();
		expect(GRAPH_RESTORE_FINISHED).one();
		message = call(() -> client().invokeRestore());
		awaitEvents();
		assertThat(message).matches("restore_finished");

		call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
		call(() -> client().findNodeByUuid(NEW_PROJECT_NAME, baseNodeUuid), NOT_FOUND, "project_not_found", NEW_PROJECT_NAME);
	}

	@Test
	public void testBackupInconsistentDB() throws IOException {
		final String backupDir = testContext.getOptions().getStorageOptions().getBackupDirectory();
		assertFilesInDir(backupDir, 0);

		GenericMessageResponse message = adminCall(() -> client().invokeBackup(new BackupParametersImpl().setConsistencyCheck(true)));
		assertThat(message).matches("backup_finished");

		// Now produce inconsistency
		Node bogusNode = tx(tx -> {
			Node bogus = tx.getGraph().addFramedVertex(NodeImpl.class);
			bogus.setUuid(UUIDUtil.randomUUID());
			bogus.setProject(project());
			return bogus;
		});

		// Re-Run the backup and expect a failure
		adminCall(() -> client().invokeBackup(new BackupParametersImpl().setConsistencyCheck(true)), INTERNAL_SERVER_ERROR, "backup_consistency_check_failed", "1");
		assertFilesInDir(backupDir, 1);

		// Remove the node to avoid test consistency check errors
		tx(tx -> {
			bogusNode.remove();
		});
	}

	@Test
	@Ignore("Endpoint disabled")
	public void testExportImport() {
		grantAdmin();
		GenericMessageResponse message = call(() -> client().invokeExport());
		assertThat(message).matches("export_finished");

		message = call(() -> client().invokeImport());
		assertThat(message).matches("import_finished");
	}

}
