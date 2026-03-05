package com.gentics.mesh.plugin.forms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Test cases for validation on form data endpoints (endpoints that accept a form data uuid)
 */
@RunWith(value = Parameterized.class)
public class FormDataEndpointValidationTest extends AbstractEndpointValidationTest {
	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {"Bogus UUID for existing Form", FORM_UUID, BOGUS_UUID, 400});
		data.add(new Object[] {"Unknown UUID for existing Form", FORM_UUID, RANDOM_UUID, 404});
		data.add(new Object[] {"Foreign UUID for existing Form", FORM_UUID, FOREIGN_UUID, 404});

		data.add(new Object[] {"Bogus Form UUID for existing Form Data", BOGUS_UUID, FORM_DATA_UUID, 400});
		data.add(new Object[] {"Unknown Form UUID for existing Form Data", RANDOM_UUID, FORM_DATA_UUID, 404});
		data.add(new Object[] {"Foreign Form UUID for existing Form Data", FOREIGN_UUID, FORM_DATA_UUID, 404});

		return data;
	}

	/**
	 * Supplier for the form uuid
	 */
	@Parameter(1)
	public Supplier<String> formUuidSupplier;

	/**
	 * Supplier for the form data uuid
	 */
	@Parameter(2)
	public Supplier<String> formDataUuidSupplier;

	/**
	 * Expected response code
	 */
	@Parameter(3)
	public int expectedResponseCode;

	/**
	 * Test loading form data
	 * @throws IOException
	 */
	@Test
	public void testLoadData() throws IOException {
		Request loadRequest = request(false).get().build();
		Response response = client().newCall(loadRequest).execute();
		assertResponse(response, expectedResponseCode);
	}

	/**
	 * Test rendering form data
	 * with header <code>Accept: text/html</code>
	 * @throws IOException
	 */
	@Test
	public void testRenderData() throws IOException {
		Request loadRequest = request(false).header("Accept", "text/html").get().build();
		Response response = client().newCall(loadRequest).execute();
		assertResponse(response, expectedResponseCode);
	}

	/**
	 * Test updating form data
	 * @throws IOException
	 */
	@Test
	public void testUpdateData() throws IOException {
		Request postRequest = request(false).post(DUMMY_REQUEST_BODY).build();
		Response response = client().newCall(postRequest).execute();
		assertResponse(response, expectedResponseCode);
	}

	/**
	 * Test updating form data as JSON
	 * @throws IOException
	 */
	@Test
	public void testUpdateJsonData() throws IOException {
		Request postRequest = request(true).post(DUMMY_REQUEST_BODY).build();
		Response response = client().newCall(postRequest).execute();
		assertResponse(response, expectedResponseCode);
	}

	/**
	 * Test deleting form data
	 * @throws IOException
	 */
	@Test
	public void testDeleteData() throws IOException {
		Request deleteRequest = request(false).delete().build();
		Response response = client().newCall(deleteRequest).execute();
		assertResponse(response, expectedResponseCode);
	}

	/**
	 * Create a request builder for a form data endpoint
	 * @param json true for jsondata instead of data
	 * @return request builder
	 */
	protected Request.Builder request(boolean json) {
		return new Request.Builder()
				.url(HttpUrl.get(projectUrl("/forms/%s/%s/%s".formatted(formUuidSupplier.get(),
						json ? "jsondata" : "data", formDataUuidSupplier.get()))).newBuilder().build())
				.header("Referer", "ref");
	}
}
