package com.gentics.mesh.core.ssl;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.SSLTestMode.CLIENT_CERT_REQUEST;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true, ssl = CLIENT_CERT_REQUEST)
public class SSLUserCertServerTest extends AbstractMeshTest {

	@Test
	public void testReadByUUID() throws Exception {
		String uuid = userUuid();
		call(() -> sslClient().findUserByUuid(uuid));

		SSLTestClient.call(httpsPort());
	}

}
