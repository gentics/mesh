package com.gentics.mesh.rest;

import org.junit.Test;

public class MeshRestClientTest {

	@Test
	public void testRestClient() {
		MeshRestClient client = new MeshRestClient("localhost", 8080);
		client.close();
	}
}
