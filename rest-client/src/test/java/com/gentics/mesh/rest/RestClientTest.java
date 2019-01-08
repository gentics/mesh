package com.gentics.mesh.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gentics.mesh.rest.client.impl.MeshRestOkHttpClientImpl;
import org.junit.Test;

import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.rest.client.AbstractMeshRestHttpClient;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.core.Vertx;

public class RestClientTest {

	@Test
	public void testRestClient() {
		MeshRestClient client = new MeshRestOkHttpClientImpl("localhost", 8080, false, Vertx.vertx());
		client.close();
	}

	@Test
	public void testParameterHandling() {
		NodeParameters parameters1 = mock(NodeParameters.class);
		when(parameters1.getLanguages()).thenReturn(new String[] { "en" });
		assertThat(parameters1.getLanguages()).contains("en");
		when(parameters1.getQueryParameters()).thenReturn("lang=en");
		assertEquals("?lang=en", AbstractMeshRestHttpClient.getQuery(parameters1));

		NodeParameters parameters2 = mock(NodeParameters.class);

		when(parameters2.getExpandedFieldNames()).thenReturn(new String[] { "test" });
		assertThat(parameters2.getExpandedFieldNames()).contains("test");

		when(parameters2.getQueryParameters()).thenReturn("expand=test");
		assertEquals("?lang=en&expand=test", AbstractMeshRestHttpClient.getQuery(parameters1, parameters2));

		assertEquals("", AbstractMeshRestHttpClient.getQuery());
	}
}
