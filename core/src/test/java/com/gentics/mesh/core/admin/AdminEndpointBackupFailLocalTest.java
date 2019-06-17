package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = NONE, testSize = FULL, startServer = true, inMemoryDB = false)
public class AdminEndpointBackupFailLocalTest extends AbstractMeshTest {

	/**
	 * Test what happens when invoking restore without backup
	 * 
	 * @throws IOException
	 */
	@Test
	public void testRestoreWithoutBackup() throws IOException {
		final String backupDir = testContext.getOptions().getStorageOptions().getBackupDirectory();
		org.apache.commons.io.FileUtils.deleteDirectory(new File(backupDir));
		new File(backupDir).delete();
		grantAdminRole();
		call(() -> client().invokeRestore(), INTERNAL_SERVER_ERROR, "error_backup", new File(backupDir).getAbsolutePath());
	}

}
