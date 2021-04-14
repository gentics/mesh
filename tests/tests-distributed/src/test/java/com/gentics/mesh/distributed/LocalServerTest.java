package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static org.junit.Assert.assertNotNull;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.test.local.MeshLocalServer;

@Ignore("Is no longer working due to maven dep issue. Should be removed")
public class LocalServerTest {

	private static String clusterPostFix = randomUUID();

	@ClassRule
	public static MeshLocalServer serverA = new MeshLocalServer()
		.withNodeName("localNodeA")
		.withClusterName("cluster" + clusterPostFix)
		.withInitCluster()
		.waitForStartup();

	@Test
	public void testServer() {
		serverA.client().setLogin("admin", "admin");
		serverA.client().login().blockingGet();
		UserListResponse users = call(() -> serverA.client().findUsers());
		assertNotNull(users);
	}

}
