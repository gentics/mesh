package com.gentics.mesh.core.ssl;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.SSLTestMode.CLIENT_CERT_REQUEST;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.core.ssl.SSLTestClient.CLIENT_CERT;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true, ssl = CLIENT_CERT_REQUEST)
public class SSLUserCertRequestServerTest extends AbstractMeshTest {

	@Test
	public void testReadByUUID() throws Exception {
		String uuid = userUuid();
		call(() -> sslClient().findUserByUuid(uuid));

		SSLTestClient.call(httpsPort(), false, CLIENT_CERT.ALICE);

		// The request should also work when not sending a client cert since the server was configured with ClientAuth.REQUEST
		SSLTestClient.call(httpsPort(), false, null);

		// It should also work when trusting any server cert
		SSLTestClient.call(httpsPort(), true, null);

		// Bob's cert does not match the server key but the request still works since the request will be processed without the client cert
		SSLTestClient.call(httpsPort(), false, CLIENT_CERT.BOB);
	}

}
