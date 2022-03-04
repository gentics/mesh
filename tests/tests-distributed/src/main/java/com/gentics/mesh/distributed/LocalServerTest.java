package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshOptionsTypeAwareContext;
import com.gentics.mesh.test.local.MeshLocalServer;

public abstract class LocalServerTest<T extends MeshOptions> implements MeshOptionsTypeAwareContext<T> {

	private static String clusterPostFix = randomUUID();

	@BeforeClass
	public void initServerA() {
		serverA = new MeshLocalServer(getOptions())
				.withNodeName("localNodeA")
				.withClusterName("cluster" + clusterPostFix)
				.withInitCluster()
				.waitForStartup();
	}
	
	public static MeshLocalServer serverA;

	@Test
	public void testServer() {
		serverA.client().setLogin("admin", "admin");
		serverA.client().login().blockingGet();
		UserListResponse users = call(() -> serverA.client().findUsers());
		assertNotNull(users);
	}
}
