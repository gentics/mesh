package com.gentics.mesh.core.rest;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.node.NodeVerticleTest;
import com.gentics.mesh.core.verticle.user.UserVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MeshRestAPITest extends AbstractRestVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(NodeVerticleTest.class);

	private UserVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void test404Response() throws Exception {
		//		Future<UserResponse> future = getClient().findUserByUuid("blub");
		//		latchFor(future);

		HttpClientOptions options = new HttpClientOptions();
		options.setDefaultHost("localhost");
		options.setDefaultPort(port);

		HttpClient client = Mesh.vertx().createHttpClient(options);

		CompletableFuture<String> future = new CompletableFuture<>();
		HttpClientRequest request = client.request(HttpMethod.POST, "/api/v1/test", rh -> {
			rh.bodyHandler(bh -> {
				future.complete(bh.toString());
			});
		});
		request.end();

		String response = future.get(1, TimeUnit.SECONDS);
		System.out.println(response);
		assertTrue("The response string should not contain any html specific characters but it was {" + response + "} ", response.indexOf("<") != 0);
	}

}
