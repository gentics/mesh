package com.gentics.mesh.core.admin;

import static com.gentics.mesh.MeshStatus.READY;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.context.OrientDBMeshOptionsProvider;

@MeshTestSetting(elasticsearch = NONE, testSize = FULL, startServer = true, inMemoryDB = false, optionsProvider = OrientDBMeshOptionsProvider.class)
public class AdminEndpointBackupFail2LocalTest extends AbstractMeshTest {

	/**
	 * Test what happens when invoking restore without backup
	 */
	@Test
	public void testBackupWithoutDir() throws IOException {
		// Use an file as backup dir to provoke an error
		File testFile = new File("target/test" + System.currentTimeMillis());
		((OrientDBMeshOptions) testContext.getOptions()).getStorageOptions().setBackupDirectory(testFile.getAbsolutePath());
		testFile.createNewFile();
		testFile.deleteOnExit();
		grantAdmin();

		// Override the current status
		status(READY);
		assertEquals(READY, status());
		call(() -> client().invokeBackup(), INTERNAL_SERVER_ERROR, "backup_failed");
		assertEquals(READY, status());
	}
}
