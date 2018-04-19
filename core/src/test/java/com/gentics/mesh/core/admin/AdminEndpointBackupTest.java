package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.assertMessage;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true, inMemoryDB = false)
public class AdminEndpointBackupTest extends AbstractMeshTest {

	@Test
	public void testBackupRestore() throws IOException {
		final String NEW_PROJECT_NAME = "enemenemuh";

		try (Tx tx = tx()) {
			group().addRole(roles().get("admin"));
			tx.success();
		}
		GenericMessageResponse message = call(() -> client().invokeBackup());
		assertMessage(message, "backup_finished");

		// Now create a project which is not in the backup. The routes and data must vanish when inserting the backup
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(NEW_PROJECT_NAME);
		request.setSchemaRef("folder");
		ProjectResponse projectResponse = call(() -> client().createProject(request));
		String baseNodeUuid = projectResponse.getRootNode().getUuid();
		call(() -> client().findNodeByUuid(NEW_PROJECT_NAME, baseNodeUuid));

		message = call(() -> client().invokeRestore());
		assertMessage(message, "restore_finished");

		call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
		call(() -> client().findNodeByUuid(NEW_PROJECT_NAME, baseNodeUuid), NOT_FOUND, "project_not_found", NEW_PROJECT_NAME);
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
