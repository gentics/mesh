package com.gentics.mesh.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.rest.client.AbstractMeshRestHttpClient;
import com.gentics.mesh.rest.client.MeshRestClient;

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
		assertEquals("?lang=en", AbstractMeshRestHttpClient.getQuery(parameters1));

		NodeParameters parameters2 = mock(NodeParameters.class);

		when(parameters2.getExpandedFieldNames()).thenReturn(new String[] { "test" });
		assertThat(parameters2.getExpandedFieldNames()).contains("test");

		when(parameters2.getQueryParameters()).thenReturn("expand=test");
		assertEquals("?lang=en&expand=test", AbstractMeshRestHttpClient.getQuery(parameters1, parameters2));

		assertEquals("", AbstractMeshRestHttpClient.getQuery());
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
		assertThat(caught.get()).as("Caught exception").isNotNull().hasMessage(
				"I/O Error in GET http://does.not.exist:4711/api/v1/ : UnknownHostException (does.not.exist)");
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
		assertThat(caught.get()).as("Caught exception").isNotNull().hasMessage(
				"I/O Error in GET http://localhost:4711/api/v1/ : ConnectException (Failed to connect to localhost/0:0:0:0:0:0:0:1:4711)");
	}
}
