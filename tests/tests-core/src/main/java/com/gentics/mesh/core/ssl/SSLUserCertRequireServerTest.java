package com.gentics.mesh.core.ssl;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.SSLTestMode.CLIENT_CERT_REQUIRED;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.fail;

import com.gentics.mesh.core.ssl.SSLTestClient.ClientCert;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import org.junit.Test;

@MeshTestSetting(testSize = FULL, startServer = true, ssl = CLIENT_CERT_REQUIRED)
public class SSLUserCertRequireServerTest extends AbstractMeshTest {

	@Test
	public void testReadByUUID() throws Exception {
		String uuid = userUuid();
		call(() -> sslClient().findUserByUuid(uuid));
		call(() -> client().findUserByUuid(uuid));
	}

	@Test
	public void givenValidCertShouldNotThrow() {
		Throwable thrownException = catchThrowable(
				() -> SSLTestClient.call(httpsPort(), false, ClientCert.ALICE));

		if (thrownException != null) {
			fail("The request should not fail since a valid request was issued.");
		}
	}

	@Test
	public void givenNoCertShouldThrow() {
		Throwable thrownException = catchThrowable(() -> SSLTestClient.call(httpsPort(), false, null));
		if (thrownException == null) {
			fail("The request should fail since no valid client certificate was passed along.");
		}
	}

	@Test
	public void givenInvalidCertShouldThrow() {
		Throwable thrownException = catchThrowable(
				() -> SSLTestClient.call(httpsPort(), false, ClientCert.BOB));

		if (thrownException == null) {
			fail("The request should fail since bob's certificate is invalid.");
		}
	}


}
