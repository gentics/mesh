package com.gentics.mesh.core.ssl;

import java.io.IOException;
import java.net.URISyntaxException;

import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.impl.OkHttpClientUtil;
import com.gentics.mesh.test.context.MeshTestHelper;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SSLTestClient {

	public enum ClientCert {
		BOB,
		ALICE;
	}

	private static final Logger log = LoggerFactory.getLogger(SSLTestClient.class);

	public static final String CERT_PATH = "/client-ssl/";

	public static final String FMT_TEST_URL = "https://localhost:%s/api/v1";

	/**
	 * Invoke call to /api/v1
	 * 
	 * @param httpsPort
	 * @param trustAll
	 * @param clientCertName
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	public static void call(int httpsPort, boolean trustAll, ClientCert clientCertName) throws IOException, URISyntaxException {
		OkHttpClient client = client(clientCertName, trustAll);
		Request request = new Request.Builder().url(String.format(FMT_TEST_URL, httpsPort)).build();

		log.info("Performing request: " + request);
		Response response = client.newCall(request).execute();
		log.info("Received response: " + response);
	}

	public static OkHttpClient client(ClientCert clientCertName, boolean trustAll) throws URISyntaxException {
		com.gentics.mesh.rest.client.MeshRestClientConfig.Builder builder = new MeshRestClientConfig.Builder();

		builder.setHost("localhost");
		if (clientCertName != null) {
			builder.setClientKey(MeshTestHelper.getResourcePath(CERT_PATH + clientCertName.name().toLowerCase() + ".key"));
			builder.setClientCert(MeshTestHelper.getResourcePath(CERT_PATH + clientCertName.name().toLowerCase() + ".pem"));
		}
		if (!trustAll) {
			builder.addTrustedCA(MeshTestHelper.getResourcePath(CERT_PATH + "server.pem"));
		}
		return OkHttpClientUtil.createClient(builder.build());

	}
}
