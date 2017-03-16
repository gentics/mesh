package com.gentics.mesh.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.rest.client.AbstractMeshRestHttpClient;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.core.Vertx;

public class MeshRestClientTest {

	@Test
	public void testRestClient() {
		MeshRestClient client = MeshRestClient.create("localhost", 8080, Vertx.vertx());
		client.close();
	}

	@Test
	public void testParameterHandling() {
		NodeParametersImpl parameters1 = new NodeParametersImpl();
		parameters1.setLanguages("en");

		NodeParametersImpl parameters2 = new NodeParametersImpl();
		parameters2.setExpandedFieldNames("test");
		assertEquals("?lang=en&expand=test", AbstractMeshRestHttpClient.getQuery(parameters1, parameters2));
		assertEquals("?lang=en", AbstractMeshRestHttpClient.getQuery(parameters1));
		assertEquals("", AbstractMeshRestHttpClient.getQuery());
	}
}
