package com.gentics.mesh.core.admin;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true, startStorageServer = true, clusterMode = false, inMemoryDB = true)
public class AdminEndpointBackupMemoryTest extends AbstractMeshTest {

	@Test
	public void testBackup() throws IOException {
		grantAdminRole();
		call(() -> client().invokeBackup(), SERVICE_UNAVAILABLE, "backup_error_not_supported_in_memory_mode");
	}

}
