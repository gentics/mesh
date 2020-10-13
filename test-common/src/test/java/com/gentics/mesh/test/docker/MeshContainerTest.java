package com.gentics.mesh.test.docker;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.junit.Assert.assertEquals;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserListResponse;

public class MeshContainerTest {

	@ClassRule
	public static MeshContainer server = new MeshContainer("gentics/mesh:1.6.0")
		.withDebug(9200)
		.waitForStartup();

	@Test
	public void testDockerImage() {
		UserListResponse response = call(() -> server.client().findUsers());
		assertEquals(2, response.getData().size());
	}

}
