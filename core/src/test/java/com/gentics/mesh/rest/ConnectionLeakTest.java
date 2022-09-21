package com.gentics.mesh.rest;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshWebrootFieldResponse;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import com.gentics.mesh.test.ConnectionVerifier;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.buffer.Buffer;

/**
 * Tests for possible resource leaks when using the MeshRestClient
 */
@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class ConnectionLeakTest extends AbstractMeshTest {
	public final static String BINARY_SCHEMA_NAME = "binary_schema";

	public final static String BINARY_FIELD_NAME = "binary";

	public final static String CONTENT_TYPE = "image/png";

	public final static String FILENAME = "somefile.png";

	public final static String WEBROOT_PATH = "/" + FILENAME;

	protected NodeResponse node;

	protected int uploadSize;

	@Rule
	public ConnectionVerifier connectionVerifier = new ConnectionVerifier(MeshTestContext.okHttp);

	/**
	 * Setup everything required for the test
	 * @throws IOException
	 */
	@Before
	public void setup() throws IOException {
		setupBinarySchema();
		setupBinaryData();
	}

	/**
	 * Setup the binary schema
	 */
	public void setupBinarySchema() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName(BINARY_SCHEMA_NAME);
		request.setFields(Arrays.asList(new BinaryFieldSchemaImpl().setName(BINARY_FIELD_NAME)));
		request.setSegmentField(BINARY_FIELD_NAME);
		SchemaResponse schemaResponse = client().createSchema(request).blockingGet();
		client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()).blockingAwait();
	}

	/**
	 * Prepare the binary data for testing
	 * @throws IOException
	 */
	public void setupBinaryData() throws IOException {
		ProjectResponse project = call(() -> client().findProjectByName(PROJECT_NAME));
		node = call(() -> client().createNode(PROJECT_NAME, new NodeCreateRequest().setLanguage("en")
				.setParentNodeUuid(project.getRootNode().getUuid()).setSchemaName(BINARY_SCHEMA_NAME)));

		InputStream ins = getClass().getResourceAsStream("/pictures/blume.jpg");
		byte[] bytes = IOUtils.toByteArray(ins);
		Buffer buffer = Buffer.buffer(bytes);

		node = call(() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "draft", BINARY_FIELD_NAME,
				new ByteArrayInputStream(buffer.getBytes()), buffer.length(),
				FILENAME, CONTENT_TYPE));
		uploadSize = buffer.length();
	}

	// Test cases for node requests (non-blocking)

	/**
	 * Test loading a node
	 * @throws InterruptedException
	 */
	@Test
	public void testNode() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		client().findNodeByUuid(PROJECT_NAME, node.getUuid()).toSingle().doFinally(() -> {
			latch.countDown();
		}).subscribe();
		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
	}

	/**
	 * Test error in consumption of response while loading a node
	 * @throws InterruptedException
	 */
	@Test
	public void testNodeErrorWhileConsuming() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		client().findNodeByUuid(PROJECT_NAME, node.getUuid()).toSingle().doOnSuccess(response -> {
			throw new RuntimeException("Something bad happens here");
		}).doFinally(() -> {
			latch.countDown();
		}).subscribe();
		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
	}

	// Test cases for node requests (blocking)

	/**
	 * Test loading a node in blocking manner
	 * @throws InterruptedException
	 */
	@Test
	public void testNodeBlocking() throws InterruptedException {
		client().findNodeByUuid(PROJECT_NAME, node.getUuid()).toSingle().blockingGet();
	}

	// Test cases for binary field (non-blocking)

	/**
	 * Test normal download of binary field
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testBinaryField() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger downloadSize = new AtomicInteger();
		client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", BINARY_FIELD_NAME).toSingle()
				.flatMapObservable(binResponse -> binResponse.getFlowable().toObservable().doOnComplete(() -> binResponse.close())).doOnNext(bytes -> {
					downloadSize.addAndGet(bytes.length);
				}).doFinally(() -> {
					latch.countDown();
				}).subscribe();

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
		assertThat(downloadSize.get()).as("Downloaded bytes").isEqualTo(uploadSize);
	}

	/**
	 * Test requesting but not downloading binary field
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testBinaryFieldNotConsumed() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", BINARY_FIELD_NAME).toSingle()
				.doAfterSuccess(response -> {
					// note: we explicitly need to close the response here
					response.close();
				}).doFinally(() -> {
					latch.countDown();
				}).subscribe();

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
	}

	/**
	 * Test error while consuming response for binary field
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testBinaryFieldErrorWhileConsuming() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", BINARY_FIELD_NAME).toSingle().doAfterSuccess(response -> {
			// note: we explicitly need to close the response here
			response.close();
		}).doOnSuccess(response -> {
			throw new RuntimeException("Something bad happens here");
		}).doFinally(() -> {
			latch.countDown();
		}).subscribe();

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
	}

	/**
	 * Test error while consuming binary data for binary field
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testBinaryFieldErrorWhileConsumingData() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		client().downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", BINARY_FIELD_NAME).toSingle()
				.flatMapObservable(binResponse -> binResponse.getFlowable().toObservable()).doOnNext(bytes -> {
					throw new RuntimeException("Something bad happens here");
				}).doFinally(() -> {
					latch.countDown();
				}).subscribe();

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
	}

	// test cases for binary field (blocking)

	/**
	 * Test downloading binary field in a blocking manner
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testBinaryFieldBlocking() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger downloadSize = new AtomicInteger();
		MeshBinaryResponse response = client()
				.downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", BINARY_FIELD_NAME).toSingle().blockingGet();
		response.getFlowable().doOnNext(bytes -> {
			downloadSize.addAndGet(bytes.length);
		}).doFinally(() -> {
			latch.countDown();
		}).subscribe();

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
		assertThat(downloadSize.get()).as("Downloaded bytes").isEqualTo(uploadSize);
	}

	/**
	 * Test requesting but not consuming binary field in a blocking manner
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testBinaryFieldBlockingNotConsumed() throws IOException, InterruptedException {
		// not we need to get the response with try-with-resources, so it automatically gets closed
		try (MeshBinaryResponse response = client()
				.downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", BINARY_FIELD_NAME).toSingle().blockingGet()) {
		}
	}

	/**
	 * Test error while downloading binary data from binary field in a blocking manner
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testBinaryFieldBlockingErrorWhileConsumingData() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		MeshBinaryResponse response = client()
				.downloadBinaryField(PROJECT_NAME, node.getUuid(), "en", BINARY_FIELD_NAME).toSingle().blockingGet();

		response.getFlowable().doOnNext(bytes -> {
			throw new RuntimeException("Something bad happens here");
		}).doFinally(() -> {
			latch.countDown();
		}).subscribe();

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
	}

	// test cases for webroot request (non-blocking)

	/**
	 * Test normal download of binary data over webroot
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebroot() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger downloadSize = new AtomicInteger();
		client().webroot(PROJECT_NAME, WEBROOT_PATH).toSingle().doFinally(() -> {
			latch.countDown();
		}).subscribe(response -> {
			if (response.isBinary()) {
				response.getBinaryResponse().getFlowable().doOnNext(bytes -> {
					downloadSize.addAndGet(bytes.length);
				}).subscribe();
			}
		});

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
		assertThat(downloadSize.get()).as("Downloaded bytes").isEqualTo(uploadSize);
	}

	/**
	 * Test requesting but not downloading over webroot
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootNotConsumed() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		client().webroot(PROJECT_NAME, WEBROOT_PATH).toSingle().doAfterSuccess(response -> {
			// note: we explicitly need to close the response here
			response.close();
		}).doFinally(() -> {
			latch.countDown();
		}).subscribe();

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
	}

	/**
	 * Test error while consuming response when downloading via webroot
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootErrorWhileConsuming() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		client().webroot(PROJECT_NAME, WEBROOT_PATH).toSingle().doAfterSuccess(response -> {
			// note: we explicitly need to close the response here
			response.close();
		}).doOnSuccess(response -> {
			throw new RuntimeException("Something bad happens here");
		}).doFinally(() -> {
			latch.countDown();
		}).subscribe();

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
	}

	/**
	 * Test error while consuming binary data when downloading via webroot
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootErrorWhileConsumingData() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		client().webroot(PROJECT_NAME, WEBROOT_PATH).toSingle().doOnSuccess(response -> {
			if (response.isBinary()) {
				response.getBinaryResponse().getFlowable().doOnNext(bytes -> {
					throw new RuntimeException("Something bad happens here");
				}).subscribe();
			}
		}).doFinally(() -> {
			latch.countDown();
		}).subscribe();

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
	}

	// test cases for webroot request (blocking)

	/**
	 * Test normal download of binary data over webroot in blocking manner
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootBlocking() throws IOException, InterruptedException {
		AtomicInteger downloadSize = new AtomicInteger();

		MeshWebrootResponse response = client().webroot(PROJECT_NAME, WEBROOT_PATH).toSingle().blockingGet();
		if (response.isBinary()) {
			response.getBinaryResponse().getFlowable().doOnNext(bytes -> {
				downloadSize.addAndGet(bytes.length);
			}).blockingSubscribe();
		}
		assertThat(downloadSize.get()).as("Downloaded bytes").isEqualTo(uploadSize);
	}

	/**
	 * Test requesting but not downloading over webroot in blocking manner
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootBlockingNotConsumed() throws IOException, InterruptedException {
		// not we need to get the response with try-with-resources, so it automatically gets closed
		try (MeshWebrootResponse response = client().webroot(PROJECT_NAME, WEBROOT_PATH).toSingle().blockingGet()) {
		}
	}

	/**
	 * Test error while consuming binary data when downloading via webroot in blocking manner
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootBlockingErrorWhileConsumingData() throws IOException, InterruptedException {
		MeshWebrootResponse response = client().webroot(PROJECT_NAME, WEBROOT_PATH).toSingle().blockingGet();

		if (response.isBinary()) {
			try {
				response.getBinaryResponse().getFlowable().doOnNext(bytes -> {
					throw new RuntimeException("Something bad happens here");
				}).blockingSubscribe();
			} catch (RuntimeException ignored) {
			}
		}
	}

	// test cases for webrootfield request (non-blocking)

	/**
	 * Test normal download of binary data over webrootfield
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootField() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicInteger downloadSize = new AtomicInteger();
		client().webrootField(PROJECT_NAME, BINARY_FIELD_NAME, WEBROOT_PATH).toSingle().doFinally(() -> {
			latch.countDown();
		}).subscribe(response -> {
			if (response.isBinary()) {
				response.getBinaryResponse().getFlowable().doOnNext(bytes -> {
					downloadSize.addAndGet(bytes.length);
				}).subscribe();
			}
		});

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
		assertThat(downloadSize.get()).as("Downloaded bytes").isEqualTo(uploadSize);
	}

	/**
	 * Test requesting but not downloading over webrootfield
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootFieldNotConsumed() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		client().webrootField(PROJECT_NAME, BINARY_FIELD_NAME, WEBROOT_PATH).toSingle().doAfterSuccess(response -> {
			// note: we explicitly need to close the response here
			response.close();
		}).doFinally(() -> {
			latch.countDown();
		}).subscribe();

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
	}

	/**
	 * Test error while consuming response when downloading via webrootfield
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootFieldErrorWhileConsuming() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		client().webrootField(PROJECT_NAME, BINARY_FIELD_NAME, WEBROOT_PATH).toSingle().doAfterSuccess(response -> {
			// note: we explicitly need to close the response here
			response.close();
		}).doOnSuccess(response -> {
			throw new RuntimeException("Something bad happens here");
		}).doFinally(() -> {
			latch.countDown();
		}).subscribe();

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
	}

	/**
	 * Test error while consuming binary data when downloading via webrootfield
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootFieldErrorWhileConsumingData() throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		client().webrootField(PROJECT_NAME, BINARY_FIELD_NAME, WEBROOT_PATH).toSingle().doOnSuccess(response -> {
			if (response.isBinary()) {
				response.getBinaryResponse().getFlowable().doOnNext(bytes -> {
					throw new RuntimeException("Something bad happens here");
				}).subscribe();
			}
		}).doFinally(() -> {
			latch.countDown();
		}).subscribe();

		assertThat(latch.await(1, TimeUnit.MINUTES)).as("Call ended in time").isTrue();
	}

	// test cases for webrootfield request (blocking)

	/**
	 * Test normal download of binary data over webrootfield in blocking manner
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootFieldBlocking() throws IOException, InterruptedException {
		AtomicInteger downloadSize = new AtomicInteger();

		MeshWebrootFieldResponse response = client().webrootField(PROJECT_NAME, BINARY_FIELD_NAME, WEBROOT_PATH).toSingle().blockingGet();
		if (response.isBinary()) {
			response.getBinaryResponse().getFlowable().doOnNext(bytes -> {
				downloadSize.addAndGet(bytes.length);
			}).blockingSubscribe();
		}
		assertThat(downloadSize.get()).as("Downloaded bytes").isEqualTo(uploadSize);
	}

	/**
	 * Test requesting but not downloading over webrootfield in blocking manner
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootFieldBlockingNotConsumed() throws IOException, InterruptedException {
		// not we need to get the response with try-with-resources, so it automatically gets closed
		try (MeshWebrootFieldResponse response = client().webrootField(PROJECT_NAME, BINARY_FIELD_NAME, WEBROOT_PATH).toSingle().blockingGet()) {
		}
	}

	/**
	 * Test error while consuming binary data when downloading via webrootfield in blocking manner
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testWebrootFieldBlockingErrorWhileConsumingData() throws IOException, InterruptedException {
		MeshWebrootFieldResponse response = client().webrootField(PROJECT_NAME, BINARY_FIELD_NAME, WEBROOT_PATH).toSingle().blockingGet();

		if (response.isBinary()) {
			try {
				response.getBinaryResponse().getFlowable().doOnNext(bytes -> {
					throw new RuntimeException("Something bad happens here");
				}).blockingSubscribe();
			} catch (RuntimeException ignored) {
			}
		}
	}
}
