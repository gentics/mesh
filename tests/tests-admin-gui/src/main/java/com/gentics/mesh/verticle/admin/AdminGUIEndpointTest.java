package com.gentics.mesh.verticle.admin;

import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.vertx.core.http.HttpClientResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.http.HttpClient;

@MeshTestSetting(testSize = TestSize.EMPTY, startServer = false)
public class AdminGUIEndpointTest extends AbstractMeshTest {

	@Before
	public void setupVerticle() throws Exception {
		EndpointRegistry registry = mesh().endpointRegistry();
		registry.register(AdminGUIEndpoint.class);
		registry.register(AdminGUI2Endpoint.class);
	}

	@BeforeClass
	public static void cleanupConfig() {
		new File(AdminGUIEndpoint.CONF_FILE).delete();
		new File(AdminGUI2Endpoint.CONF_FILE).delete();
	}

	@Test
	public void testAdminConfigRendering() throws InterruptedException, ExecutionException, TimeoutException {

		HttpClient client = createHttpClient();
		CompletableFuture<String> future = new CompletableFuture<>();
		client.request(GET, "/mesh-ui-v1/mesh-ui-config.js")
				.compose(request -> request.send())
				.onComplete(rh -> {
					HttpClientResponse response = rh.result();
					response.bodyHandler(bh -> {
						if (response.statusCode() == 200) {
							future.complete(bh.toString());
						} else {
							future.completeExceptionally(new Exception("Status code wrong {" + response.statusCode() + "}"));
						}
					});
				});

		String response = future.get(10, TimeUnit.SECONDS);
		// String expectedUrl = "localhost:" + port;
		// assertTrue("The meshConfig.js file did not contain the expected url {" + expectedUrl + "} Response {" + response + "}",
		// response.contains(expectedUrl));
		// System.out.println(response);
		assertTrue("The response string should not contain any html specific characters but it was {" + response + "} ", response.indexOf("<") != 0);

	}

	@Test
	public void testRedirect() throws InterruptedException, ExecutionException {
		HttpClient client = createHttpClient();
		CompletableFuture<String> future = new CompletableFuture<>();
		client.request(GET, "/")
				.compose(request -> request.send())
				.onComplete(rh -> {
					future.complete(rh.result().getHeader("Location"));
				});
		assertEquals("/mesh-ui/", future.get());
	}
}
