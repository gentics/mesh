package com.gentics.mesh.rest;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.VertxOptions;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;

/**
 * Test with an increased max http header size.
 */
@MeshTestSetting(testSize = PROJECT, startServer = true, customOptionChanger = MaxHeaderSizeTest.class)
public class MaxHeaderSizeTest extends MaxHeaderSizeDefaultTest implements MeshOptionChanger {

	public static final int CUSTOM_MAX_HEADER_SIZE = 4 * VertxOptions.DEFAULT_MAX_HEADER_SIZE;

	@Override
	public void change(MeshOptions options) {
		options.getVertxOptions().setMaxHeaderSize(CUSTOM_MAX_HEADER_SIZE);
	}

	@Test
	public void testCustomMaxHeaderSizeApplied() {
		assertEquals(CUSTOM_MAX_HEADER_SIZE, maxHeaderSize());
	}

	@Test
	public void testRestHeaderAboveDefaultLimit() throws IOException {
		int code = getWithHeaderSize(httpClient(), restUrl(), VertxOptions.DEFAULT_MAX_HEADER_SIZE + HEADER_SIZE_MARGIN);
		assertNotEquals("A header above the default but below the configured max header size should be accepted", REQUEST_HEADER_FIELDS_TOO_LARGE, code);
	}

	@Test
	public void testMonitoringHeaderAboveDefaultLimit() throws IOException {
		int code = getWithHeaderSize(httpClient(), monitoringUrl(), VertxOptions.DEFAULT_MAX_HEADER_SIZE + HEADER_SIZE_MARGIN);
		assertEquals("A header above the default but below the configured max header size should be accepted", 200, code);
	}
}
