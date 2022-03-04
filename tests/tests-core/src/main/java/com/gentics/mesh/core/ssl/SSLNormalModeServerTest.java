package com.gentics.mesh.core.ssl;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.SSLTestMode.NORMAL;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true, ssl = NORMAL)
public class SSLNormalModeServerTest extends AbstractMeshTest {

	@Test
	public void testReadByUUID() throws Exception {
		String uuid = userUuid();
		call(() -> client().findUserByUuid(uuid));
	}

}
