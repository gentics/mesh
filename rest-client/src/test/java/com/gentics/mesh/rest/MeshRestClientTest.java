package com.gentics.mesh.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.query.impl.NodeRequestParameter;

import io.vertx.core.Vertx;

public class MeshRestClientTest {

	@Test
	public void testRestClient() {
		MeshRestClient client = MeshRestClient.create("localhost", 8080, Vertx.vertx());
		client.close();
	}

	@Test
	public void testParameterHandling() {
		MeshRestClient client = MeshRestClient.create("localhost", 8080, Vertx.vertx());
		NodeRequestParameter parameters1 = new NodeRequestParameter();
		parameters1.setLanguages("en");

		NodeRequestParameter parameters2 = new NodeRequestParameter();
		parameters2.setExpandedFieldNames("test");
		assertEquals("?lang=en&expand=test", AbstractMeshRestClient.getQuery(parameters1, parameters2));
		assertEquals("?lang=en", AbstractMeshRestClient.getQuery(parameters1));
		assertEquals("", AbstractMeshRestClient.getQuery());
	}
}
