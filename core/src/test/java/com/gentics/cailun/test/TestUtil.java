package com.gentics.cailun.test;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.ServerSocket;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public final class TestUtil {

	private TestUtil() {

	}

	/**
	 * Not the most elegant or efficient solution, but works.
	 * 
	 * @param port
	 * @return
	 */
	public static int getRandomPort() {
		ServerSocket socket = null;

		try {
			socket = new ServerSocket(0);
			return socket.getLocalPort();
		} catch (IOException ioe) {
			return -1;
		} finally {
			// if we did open it cause it's available, close it
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					// ignore
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Compare both json strings but remove the uuids from the unsanitizedResponseJson before comparison.
	 * 
	 * @param expectedJson
	 * @param unsanitizedResponseJson
	 * @param modelClazz
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static <T extends AbstractRestModel> void assertEqualsSanitizedJson(String expectedJson, String unsanitizedResponseJson,
			Class<T> modelClazz) throws JsonGenerationException, JsonMappingException, IOException {
		T responseObject = new ObjectMapper().readValue(unsanitizedResponseJson, modelClazz);
		assertNotNull(responseObject);
		// Update the uuid and compare json afterwards
		responseObject.setUuid("uuid-value");
		String sanitizedJson = new ObjectMapper().writeValueAsString(responseObject);
		org.junit.Assert.assertEquals("The response json did not match the expected one.", expectedJson, sanitizedJson);
	}
}
