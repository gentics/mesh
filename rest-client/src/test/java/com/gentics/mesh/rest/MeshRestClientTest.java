package com.gentics.mesh.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.etc.config.AuthenticationOptions.AuthenticationMethod;
import com.gentics.mesh.parameter.impl.NodeParameters;

import io.vertx.core.Vertx;

public class MeshRestClientTest {

	@Test
	public void testRestClient() {
		MeshRestClient client = MeshRestClient.create("localhost", 8080, Vertx.vertx(), AuthenticationMethod.BASIC_AUTH);
		client.close();
	}

	@Test
	public void testParameterHandling() {
		MeshRestClient client = MeshRestClient.create("localhost", 8080, Vertx.vertx(), AuthenticationMethod.BASIC_AUTH);
		NodeParameters parameters1 = new NodeParameters();
		parameters1.setLanguages("en");

		NodeParameters parameters2 = new NodeParameters();
		parameters2.setExpandedFieldNames("test");
		assertEquals("?lang=en&expand=test", AbstractMeshRestHttpClient.getQuery(parameters1, parameters2));
		assertEquals("?lang=en", AbstractMeshRestHttpClient.getQuery(parameters1));
		assertEquals("", AbstractMeshRestHttpClient.getQuery());
	}
}
