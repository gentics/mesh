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
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true, inMemoryDB = false, clusterMode = true)
public class AdminEndpointBackupClusteredTest extends AbstractMeshTest {

	@Test
	public void testBackup() throws IOException {
		final String NEW_PROJECT_NAME = "enemenemuh";
		final String backupDir = testContext.getOptions().getStorageOptions().getBackupDirectory();

		assertFilesInDir(backupDir, 0);
		grantAdminRole();
		GenericMessageResponse message = call(() -> client().invokeBackup());
		assertThat(message).matches("backup_finished");
		assertFilesInDir(backupDir, 1);

		// Now create a project which is not in the backup. The routes and data must vanish when inserting the backup
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(NEW_PROJECT_NAME);
		request.setSchemaRef("folder");
		ProjectResponse projectResponse = call(() -> client().createProject(request));
		String baseNodeUuid = projectResponse.getRootNode().getUuid();
		call(() -> client().findNodeByUuid(NEW_PROJECT_NAME, baseNodeUuid));
	}

	@Test
	public void testRestore() {
		call(() -> client().invokeRestore(), SERVICE_UNAVAILABLE, "restore_error_in_cluster_mode");
	}

}
