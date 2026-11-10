package com.gentics.mesh.rest;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Test that the max http header size configured in the Vert.x options is applied to the REST API and the monitoring server.
 */
@MeshTestSetting(testSize = PROJECT, startServer = true)
public class MaxHeaderSizeDefaultTest extends AbstractMeshTest {

	public static final int REQUEST_HEADER_FIELDS_TOO_LARGE = 431;

	/**
	 * Margin between the configured max header size and the size of the header sent in the tests.
	 */
	public static final int HEADER_SIZE_MARGIN = 1024;

	public static final String TEST_HEADER = "X-Test-Header";

	protected String restUrl() {
		return String.format("http://localhost:%d%s/", port(), MeshVersion.CURRENT_API_BASE_PATH);
	}

	protected String monitoringUrl() {
		return String.format("http://localhost:%d%s/health/live", options().getMonitoringOptions().getPort(), MeshVersion.CURRENT_API_BASE_PATH);
	}

	protected int maxHeaderSize() {
		return options().getVertxOptions().getMaxHeaderSize();
	}

	/**
	 * Perform a GET request to the given url with a header value of the given size and return the response status code.
	 */
	protected int getWithHeaderSize(OkHttpClient client, String url, int headerSize) throws IOException {
		Request request = new Request.Builder()
			.url(url)
			.addHeader(TEST_HEADER, StringUtils.repeat('a', headerSize))
			.build();
		try (Response response = client.newCall(request).execute()) {
			return response.code();
		}
	}

	@Test
	public void testRestHeaderBelowLimit() throws IOException {
		int code = getWithHeaderSize(httpClient(), restUrl(), maxHeaderSize() - HEADER_SIZE_MARGIN);
		assertNotEquals("A header below the max header size should be accepted", REQUEST_HEADER_FIELDS_TOO_LARGE, code);
	}

	@Test
	public void testRestHeaderAboveLimit() throws IOException {
		int code = getWithHeaderSize(httpClient(), restUrl(), maxHeaderSize() + HEADER_SIZE_MARGIN);
		assertEquals("A header above the max header size should be rejected", REQUEST_HEADER_FIELDS_TOO_LARGE, code);
	}

	@Test
	public void testMonitoringHeaderBelowLimit() throws IOException {
		int code = getWithHeaderSize(httpClient(), monitoringUrl(), maxHeaderSize() - HEADER_SIZE_MARGIN);
		assertEquals("A header below the max header size should be accepted", 200, code);
	}

	@Test
	public void testMonitoringHeaderAboveLimit() throws IOException {
		int code = getWithHeaderSize(httpClient(), monitoringUrl(), maxHeaderSize() + HEADER_SIZE_MARGIN);
		assertEquals("A header above the max header size should be rejected", REQUEST_HEADER_FIELDS_TOO_LARGE, code);
	}
}
