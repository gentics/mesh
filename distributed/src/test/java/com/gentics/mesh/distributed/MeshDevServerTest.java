package com.gentics.mesh.distributed;

import static org.junit.Assert.assertFalse;

import org.junit.Rule;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.distributed.containers.MeshDevServer;
import com.gentics.mesh.rest.client.MeshRestClient;

public class MeshDevServerTest {

	@Rule
	public MeshDevServer serverA = new MeshDevServer("localA", true, true);

	@Rule
	public MeshDevServer serverB = new MeshDevServer("localB", true, false);

	@Test
	public void testServer() throws InterruptedException {
		serverA.awaitStartup(100);
		MeshRestClient client = serverA.getMeshClient();
		UserListResponse list = client.findUsers().toSingle().toBlocking().value();
		assertFalse(list.getData().isEmpty());
		serverB.awaitStartup(100);
	}
}
