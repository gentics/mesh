package com.gentics.mesh.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.gentics.mesh.parameter.NodeParameters;
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
		NodeParameters parameters1 = mock(NodeParameters.class);
		when(parameters1.getLanguages()).thenReturn(new String[] { "en" });

		NodeParameters parameters2 = mock(NodeParameters.class);
		when(parameters2.getExpandedFieldNames()).thenReturn(new String[] { "test" });
		assertEquals("?lang=en&expand=test", AbstractMeshRestHttpClient.getQuery(parameters1, parameters2));
		assertEquals("?lang=en", AbstractMeshRestHttpClient.getQuery(parameters1));
		assertEquals("", AbstractMeshRestHttpClient.getQuery());
	}
}
