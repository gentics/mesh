package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.distributed.containers.MeshLocalServer;

public class MeshLocalServerTest {

	@ClassRule
	public static MeshLocalServer serverA = new MeshLocalServer("localNodeA", true, true);

	@Test
	public void testServer() {
		UserListResponse users = call(() -> serverA.getMeshClient().findUsers());
		assertNotNull(users);
	}

}
