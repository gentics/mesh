package com.gentics.mesh.core.admin;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
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

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true, inMemoryDB = false)
public class AdminEndpointBackupLocalTest extends AbstractMeshTest {

	@Test
	public void testBackupRestore() throws IOException {
		final String NEW_PROJECT_NAME = "enemenemuh";
		grantAdminRole();
		GenericMessageResponse message = call(() -> client().invokeBackup());
		assertThat(message).matches("backup_finished");

		// Now create a project which is not in the backup. The routes and data must vanish when inserting the backup
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(NEW_PROJECT_NAME);
		request.setSchemaRef("folder");
		ProjectResponse projectResponse = call(() -> client().createProject(request));
		String baseNodeUuid = projectResponse.getRootNode().getUuid();
		call(() -> client().findNodeByUuid(NEW_PROJECT_NAME, baseNodeUuid));

		message = call(() -> client().invokeRestore());
		assertThat(message).matches("restore_finished");

		call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
		call(() -> client().findNodeByUuid(NEW_PROJECT_NAME, baseNodeUuid), NOT_FOUND, "project_not_found", NEW_PROJECT_NAME);
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
