package com.gentics.mesh.core.rest;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;

@MeshTestSetting(testSize = PROJECT, startServer = true)
public class MeshRestAPITest extends AbstractMeshTest {

	@Test
	public void test404Response() throws Exception {
		HttpClientOptions options = new HttpClientOptions();
		options.setDefaultHost("localhost");
		options.setDefaultPort(port());

		HttpClient client = vertx().createHttpClient(options);
		CompletableFuture<String> future = new CompletableFuture<>();
		HttpClientRequest request = client.request(HttpMethod.POST, MeshVersion.CURRENT_API_BASE_PATH + "/test", rh -> {
			rh.bodyHandler(bh -> {
				future.complete(bh.toString());
			});
		});
		request.end();

		String response = future.get(1, TimeUnit.SECONDS);
		assertTrue("The response string should not contain any html specific characters but it was {" + response + "} ",
				response.indexOf("<") != 0);
	}

}
