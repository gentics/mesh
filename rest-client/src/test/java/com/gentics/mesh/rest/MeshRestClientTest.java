package com.gentics.mesh.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.node.NodeRequestParameters;

public class MeshRestClientTest {

	@Test
	public void testRestClient() {
		MeshRestClient client = new MeshRestClient("localhost", 8080, Mesh.vertx());
		client.close();
	}

	@Test
	public void testParameterHandling() {
		MeshRestClient client = new MeshRestClient("localhost", 8080, Mesh.vertx());
		NodeRequestParameters parameters1 = new NodeRequestParameters();
		parameters1.setLanguages("en");

		NodeRequestParameters parameters2 = new NodeRequestParameters();
		parameters2.setExpandedFieldNames("test");
		assertEquals("?lang=en&expand=test", client.getQuery(parameters1, parameters2));
		assertEquals("?lang=en", client.getQuery(parameters1));
		assertEquals("", client.getQuery());
	}
}
