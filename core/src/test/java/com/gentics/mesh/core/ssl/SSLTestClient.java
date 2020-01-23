package com.gentics.mesh.core.ssl;

import java.io.IOException;

import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.impl.OkHttpClientUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SSLTestClient {

	private static final Logger log = LoggerFactory.getLogger(SSLTestClient.class);

	public static final String CLIENT_CERT_PEM = "src/test/resources/client-ssl/alice.pem";
	public static final String CLIENT_KEY_PEM = "src/test/resources/client-ssl/alice.key";
	public static final String CA_CERT = "src/test/resources/client-ssl/server.pem";
	public static final String FMT_TEST_URL = "https://localhost:%s/api/v1";

	/**
	 * Invoke call to /api/v1
	 * 
	 * @param httpsPort
	 * @param sendClientAuth
	 * @param trustAll
	 * @throws IOException
	 */
	public static void call(int httpsPort, boolean sendClientAuth, boolean trustAll) throws IOException {
		OkHttpClient client = client(sendClientAuth, trustAll);
		Request request = new Request.Builder().url(String.format(FMT_TEST_URL, httpsPort)).build();

		log.info("Performing request: " + request);
		Response response = client.newCall(request).execute();
		log.info("Received response: " + response);
	}

	public static OkHttpClient client(boolean sendClientAuth, boolean trustAll) {
		com.gentics.mesh.rest.client.MeshRestClientConfig.Builder builder = new MeshRestClientConfig.Builder();

		builder.setHost("localhost");
		if (sendClientAuth) {
			builder.setClientCert(CLIENT_CERT_PEM);
			builder.setClientKey(CLIENT_KEY_PEM);
		}
		if (!trustAll) {
			builder.setTrustedCA(CA_CERT);
		}
		return OkHttpClientUtil.createClient(builder.build());

	}
}
