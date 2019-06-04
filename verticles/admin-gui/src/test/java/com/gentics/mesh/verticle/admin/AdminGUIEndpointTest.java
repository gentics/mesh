package com.gentics.mesh.verticle.admin;

import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;

@MeshTestSetting(testSize = TestSize.EMPTY, startServer = false)
public class AdminGUIEndpointTest extends AbstractMeshTest {

	@Before
	public void setupVerticle() throws Exception {
		EndpointRegistry registry = MeshInternal.get().endpointRegistry();
		registry.register(AdminGUIEndpoint.class);
	}

	@BeforeClass
	public static void cleanupConfig() {
		new File(AdminGUIEndpoint.CONF_FILE).delete();
	}

	@Test
	public void testAdminConfigRendering() throws InterruptedException, ExecutionException, TimeoutException {

		HttpClient client = createHttpClient();
		CompletableFuture<String> future = new CompletableFuture<>();
		HttpClientRequest request = client.request(GET, "/mesh-ui/mesh-ui-config.js", rh -> {
			rh.bodyHandler(bh -> {
				if (rh.statusCode() == 200) {
					future.complete(bh.toString());
				} else {
					future.completeExceptionally(new Exception("Status code wrong {" + rh.statusCode() + "}"));
				}
			});
		});
		request.end();

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
		HttpClientRequest request = client.request(GET, "/", rh -> {
			future.complete(rh.getHeader("Location"));
		});
		request.end();
		assertEquals("/mesh-ui/", future.get());
	}
}
