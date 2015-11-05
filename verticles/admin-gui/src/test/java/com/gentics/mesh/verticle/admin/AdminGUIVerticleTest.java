package com.gentics.mesh.verticle.admin;

import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;

public class AdminGUIVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private AdminGUIVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testAdminConfigRendering() throws InterruptedException, ExecutionException, TimeoutException {

		HttpClientOptions options = new HttpClientOptions();
		options.setDefaultHost("localhost");
		options.setDefaultPort(port);

		HttpClient client = Mesh.vertx().createHttpClient(options);

		CompletableFuture<String> future = new CompletableFuture<>();
		HttpClientRequest request = client.request(GET, "/mesh-ui/meshConfig.js", rh -> {
			rh.bodyHandler(bh -> {
				future.complete(bh.toString());
			});
		});
		request.end();

		String response = future.get(1, TimeUnit.SECONDS);
		String expectedUrl = "localhost:" + port;
		assertTrue("The meshConfig.js file did not contain the expected url {" + expectedUrl + "} Response {" + response + "}",
				response.contains(expectedUrl));
		System.out.println(response);
		assertTrue("The response string should not contain any html specific characters but it was {" + response + "} ", response.indexOf("<") != 0);

	}

}
