package com.gentics.mesh.core.admin;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import org.junit.Test;

import java.io.IOException;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

@MeshTestSetting(elasticsearch = NONE, testSize = FULL, startServer = true, startStorageServer = true, clusterMode = false, inMemoryDB = true)
public class AdminEndpointBackupMemoryTest extends AbstractMeshTest {

	@Test
	public void testBackup() throws IOException {
		grantAdminRole();
		call(() -> client().invokeBackup(), SERVICE_UNAVAILABLE, "backup_error_not_supported_in_memory_mode");
	}

}
