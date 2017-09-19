package com.gentics.mesh.test.docker;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.junit.Assert.assertEquals;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserListResponse;

import io.vertx.core.Vertx;

@Ignore
public class MeshDockerServerTest {
	
	@ClassRule
	public static MeshDockerServer server = new MeshDockerServer("gentics/mesh:0.10.0", "dummy", true, Vertx.vertx(), null, null);

	@Test
	public void testDockerImage() {
		UserListResponse response = call(() -> server.getMeshClient().findUsers());
		assertEquals(2, response.getData().size());
	}

}
