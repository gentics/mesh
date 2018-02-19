package com.gentics.mesh.test.docker;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.junit.Assert.assertEquals;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserListResponse;

import io.vertx.core.Vertx;

public class MeshDockerServerTest {

	@ClassRule
	public static MeshDockerServer server = new MeshDockerServer("gentics/mesh:0.16.0", Vertx.vertx())
		.withDebug(9200)
		.withNodeName("dummy")
		.waitForStartup();

	@Test
	public void testDockerImage() {
		UserListResponse response = call(() -> server.getMeshClient().findUsers());
		assertEquals(2, response.getData().size());
	}

}
