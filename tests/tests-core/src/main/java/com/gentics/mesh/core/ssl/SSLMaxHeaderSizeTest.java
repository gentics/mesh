package com.gentics.mesh.core.ssl;

import static com.gentics.mesh.rest.MaxHeaderSizeDefaultTest.HEADER_SIZE_MARGIN;
import static com.gentics.mesh.rest.MaxHeaderSizeDefaultTest.REQUEST_HEADER_FIELDS_TOO_LARGE;
import static com.gentics.mesh.rest.MaxHeaderSizeDefaultTest.TEST_HEADER;
import static com.gentics.mesh.test.SSLTestMode.NORMAL;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.VertxOptions;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Test that the configured max http header size is applied to the https server.
 */
@MeshTestSetting(testSize = PROJECT, startServer = true, ssl = NORMAL, customOptionChanger = SSLMaxHeaderSizeTest.class)
public class SSLMaxHeaderSizeTest extends AbstractMeshTest implements MeshOptionChanger {

	public static final int CUSTOM_MAX_HEADER_SIZE = 4 * VertxOptions.DEFAULT_MAX_HEADER_SIZE;

	@Override
	public void change(MeshOptions options) {
		options.getVertxOptions().setMaxHeaderSize(CUSTOM_MAX_HEADER_SIZE);
	}

	private int getWithHeaderSize(int headerSize) throws IOException, URISyntaxException {
		Request request = new Request.Builder()
			.url(String.format(SSLTestClient.FMT_TEST_URL, httpsPort()))
			.addHeader(TEST_HEADER, StringUtils.repeat('a', headerSize))
			.build();
		try (Response response = SSLTestClient.client(null, true).newCall(request).execute()) {
			return response.code();
		}
	}

	@Test
	public void testHeaderAboveDefaultLimit() throws Exception {
		int code = getWithHeaderSize(VertxOptions.DEFAULT_MAX_HEADER_SIZE + HEADER_SIZE_MARGIN);
		assertNotEquals("A header above the default but below the configured max header size should be accepted", REQUEST_HEADER_FIELDS_TOO_LARGE, code);
	}

	@Test
	public void testHeaderAboveLimit() throws Exception {
		int code = getWithHeaderSize(CUSTOM_MAX_HEADER_SIZE + HEADER_SIZE_MARGIN);
		assertEquals("A header above the max header size should be rejected", REQUEST_HEADER_FIELDS_TOO_LARGE, code);
	}
}
