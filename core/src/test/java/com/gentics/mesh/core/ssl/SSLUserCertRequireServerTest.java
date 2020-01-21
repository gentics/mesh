package com.gentics.mesh.core.ssl;

import static com.gentics.mesh.test.SSLTestMode.CLIENT_CERT_REQUIRED;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true, ssl = CLIENT_CERT_REQUIRED)
public class SSLUserCertRequireServerTest extends AbstractMeshTest {

	@Test
	public void testReadByUUID() throws Exception {
		SSLTestClient.call(httpsPort(), true);
	}

}
