package com.gentics.mesh.core.ssl;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.SSLTestMode.CLIENT_CERT_REQUIRED;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.junit.Test;

import com.gentics.mesh.core.ssl.SSLTestClient.ClientCert;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true, ssl = CLIENT_CERT_REQUIRED)
public class SSLUserCertRequireServerTest extends AbstractMeshTest {

	@Test
	public void testReadByUUID() throws Exception {
		String uuid = userUuid();
		call(() -> sslClient().findUserByUuid(uuid));
		call(() -> client().findUserByUuid(uuid));

		SSLTestClient.call(httpsPort(), false, ClientCert.ALICE);

		try {
			SSLTestClient.call(httpsPort(), false, null);
			fail("The request should fail since no valid client was passed along.");
		} catch (SSLHandshakeException e) {
			assertEquals("Received fatal alert: bad_certificate", e.getMessage());
		} catch (SSLException e) {
			assertEquals("readHandshakeRecord", e.getMessage());
		}

		// Bob's cert does not match the server key and is not accepted
		try {
			SSLTestClient.call(httpsPort(), false, ClientCert.BOB);
			fail("The request should fail since bob's cert is invalid.");
		} catch (SSLHandshakeException e) {
			assertEquals("Received fatal alert: bad_certificate", e.getMessage());
		} catch (SSLException e) {
			assertEquals("readHandshakeRecord", e.getMessage());
		}
	}

}
