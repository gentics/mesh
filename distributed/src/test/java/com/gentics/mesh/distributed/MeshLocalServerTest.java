package com.gentics.mesh.distributed;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.distributed.containers.MeshDevServer;

public class MeshLocalServerTest {

	@ClassRule
	public static MeshDevServer serverA = new MeshDevServer("nodeA", true, true);

	
	@Test
	public void testServer() {
		
	}
	
}
