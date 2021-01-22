package com.gentics.mesh.core.admin;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.context.OrientDBMeshOptionsProvider;

@MeshTestSetting(testSize = FULL, startServer = true, startStorageServer = true, clusterMode = false, inMemoryDB = false, optionsProvider = OrientDBMeshOptionsProvider.class)
public class AdminEndpointBackupServerTest extends AbstractMeshTest {

	@Test
	public void testBackupRestore() throws IOException {
		final String NEW_PROJECT_NAME = "enemenemuh";
		final String backupDir = ((OrientDBMeshOptions) testContext.getOptions()).getStorageOptions().getBackupDirectory();

		assertFilesInDir(backupDir, 0);
		GenericMessageResponse message = adminCall(() -> client().invokeBackup());
		assertThat(message).matches("backup_finished");
		assertFilesInDir(backupDir, 1);

		// Now create a project which is not in the backup. The routes and data must vanish when inserting the backup
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(NEW_PROJECT_NAME);
		request.setSchemaRef("folder");
		ProjectResponse projectResponse = call(() -> client().createProject(request));
		String baseNodeUuid = projectResponse.getRootNode().getUuid();
		call(() -> client().findNodeByUuid(NEW_PROJECT_NAME, baseNodeUuid));

		adminCall(() -> client().invokeRestore(), SERVICE_UNAVAILABLE, "restore_error_in_server_mode");
	}

}
