package com.gentics.mesh.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.rest.client.AbstractMeshRestHttpClient;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;

public class RestClientTest {

	@Test
	public void testRestClient() {
		MeshRestClient client = MeshRestClient.create("localhost", 8080, false);
		client.close();
	}

	@Test
	public void testParameterHandling() {
		NodeParameters parameters1 = mock(NodeParameters.class);
		when(parameters1.getLanguages()).thenReturn(new String[] { "en" });
		assertThat(parameters1.getLanguages()).contains("en");
		when(parameters1.getQueryParameters()).thenReturn("lang=en");
		when(parameters1.getParameters()).thenReturn(Map.of("lang", "en"));
		assertEquals("?lang=en", AbstractMeshRestHttpClient.getQuery(null, parameters1));

		NodeParameters parameters2 = mock(NodeParameters.class);

		when(parameters2.getExpandedFieldNames()).thenReturn(new String[] { "test" });
		assertThat(parameters2.getExpandedFieldNames()).contains("test");

		when(parameters2.getQueryParameters()).thenReturn("expand=test");
		when(parameters2.getParameters()).thenReturn(Map.of("expand", "test"));
		assertEquals("?lang=en&expand=test", AbstractMeshRestHttpClient.getQuery(null, parameters1, parameters2));

		assertEquals("", AbstractMeshRestHttpClient.getQuery(null));
	}

	/**
	 * Test that the error for a request to an unknown host contains meaningful information
	 * @throws InterruptedException
	 */
	@Test
	public void testHandlingUnknownHost() throws InterruptedException {
		MeshRestClient client = MeshRestClient.create("does.not.exist", 4711, false);
		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean success = new AtomicBoolean(false);
		AtomicReference<Throwable> caught = new AtomicReference<>();
		client.get("/").toSingle().subscribe(response -> {
			success.set(true);
			latch.countDown();
		}, error -> {
			caught.set(error);
			latch.countDown();
		});

		latch.await(10, TimeUnit.SECONDS);
		assertThat(success.get()).isFalse();
		assertThat(caught.get()).as("Caught exception").isNotNull().hasMessageStartingWith(
				"I/O Error in GET http://does.not.exist:4711/api/v1/ : UnknownHostException");
	}

	/**
	 * Test that the error for a connection error contains meaningful information
	 * @throws InterruptedException
	 */
	@Test
	public void testHandlingConnectError() throws InterruptedException {
		MeshRestClient client = MeshRestClient.create("localhost", 4711, false);
		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean success = new AtomicBoolean(false);
		AtomicReference<Throwable> caught = new AtomicReference<>();
		client.get("/").toSingle().subscribe(response -> {
			success.set(true);
			latch.countDown();
		}, error -> {
			caught.set(error);
			latch.countDown();
		});

		latch.await(10, TimeUnit.SECONDS);
		assertThat(success.get()).isFalse();
		assertThat(caught.get()).as("Caught exception").isNotNull().hasMessageStartingWith(
				"I/O Error in GET http://localhost:4711/api/v1/ : ConnectException");
	}

	/**
	 * Test that default parameters in the config are handled correctly
	 */
	@Test
	public void testDefaultParameter() {
		// generate a minimal config with setting etag=false per default
		MeshRestClientConfig config = MeshRestClientConfig
				.newConfig()
				.setHost("localhost")
				.setDefaultParameters(new GenericParametersImpl().setETag(false))
				.build();

		// get query without additional parameters
		assertThat(AbstractMeshRestHttpClient.getQuery(config)).as("Query").isEqualTo("?etag=false");

		// get query with additional other parameter
		assertThat(AbstractMeshRestHttpClient.getQuery(config, new GenericParametersImpl().setFields("uuid"))).as("Query").isEqualTo("?etag=false&fields=uuid");

		// get query with overridden default parameter
		assertThat(AbstractMeshRestHttpClient.getQuery(config, new GenericParametersImpl().setETag(true))).as("Query").isEqualTo("?etag=true");

		// get query with overridden default parameter
		assertThat(AbstractMeshRestHttpClient.getQuery(config, new GenericParametersImpl().setETag(true).setFields("fields,perms"))).as("Query").isEqualTo("?etag=true&fields=fields,perms");
	}
}
