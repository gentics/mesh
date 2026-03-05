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
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Test cases for validation on form endpoints (endpoints that do not accept a form data uuid)
 */
@RunWith(value = Parameterized.class)
public class FormEndpointValidationTest extends AbstractEndpointValidationTest {
	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		data.add(new Object[] {"Bogus UUID", BOGUS_UUID, 400, 400, 400, 400});
		data.add(new Object[] {"Unknown UUID", RANDOM_UUID, 404, 200, 204, 200});
		data.add(new Object[] {"Foreign UUID", FOREIGN_UUID, 404, 400, 404, 200});
		return data;
	}

	/**
	 * Supplier for the form uuid
	 */
	@Parameter(1)
	public Supplier<String> uuidSupplier;

	/**
	 * Expected response code for load endpoints
	 */
	@Parameter(2)
	public int expectedResponseCodeLoad;

	/**
	 * Expected response code for update endpoint
	 */
	@Parameter(3)
	public int expectedResponseCodeUpdate;

	/**
	 * Expected response code for delete endpoint
	 */
	@Parameter(4)
	public int expectedResponseCodeDelete;

	/**
	 * Expected response code the check download status endpoint
	 */
	@Parameter(5)
	public int expectedResponseCodeCheckDownloadStatus;

	/**
	 * Test loading a form
	 * @throws IOException
	 */
	@Test
	public void testLoadForm() throws IOException {
		Request request = request()
			.get()
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test rendering a form
	 * @throws IOException
	 */
	@Test
	public void testRenderForm() throws IOException {
		Request request = request()
			.header("Accept", "text/html")
			.get()
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test updating a form
	 * @throws IOException
	 */
	@Test
	public void testUpdateform() throws IOException {
		Request request = request()
				.post(requestBodyFromFile("update.json"))
				.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeUpdate);
	}

	/**
	 * Test deleting a form
	 * @throws IOException
	 */
	@Test
	public void testDeleteForm() throws IOException {
		Request request = request()
			.delete()
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeDelete);
	}

	/**
	 * Test checking the online status
	 * @throws IOException
	 */
	@Test
	public void testOnlineStatus() throws IOException {
		Request request = request("/online/en")
			.get()
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test setting a form online
	 * @throws IOException
	 */
	@Test
	public void testSetOnline() throws IOException {
		Request request = request("/online/en")
			.put(RequestBody.create(MediaType.get("text/plain"), ""))
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test setting a form offline
	 * @throws IOException
	 */
	@Test
	public void testSetOffline() throws IOException {
		Request request = request("/online/en")
			.delete()
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test loading form elements
	 * @throws IOException
	 */
	@Test
	public void testLoadElements() throws IOException {
		Request request = request("/elements")
			.get()
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test loading a form schema
	 * @throws IOException
	 */
	@Test
	public void testLoadSchema() throws IOException {
		Request request = request("/schema")
			.get()
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test POSTing data to a form
	 * @throws IOException
	 */
	@Test
	public void testPostData() throws IOException {
		Request request = request("/data")
			.post(DUMMY_REQUEST_BODY)
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test POSTing data to a form as JSON
	 * @throws IOException
	 */
	@Test
	public void testPostDataAsJson() throws IOException {
		Request request = request("/jsondata")
			.post(DUMMY_REQUEST_BODY)
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test getting all form data
	 * @throws IOException
	 */
	@Test
	public void testGetData() throws IOException {
		Request request = request("/data")
			.get()
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test getting an export
	 * @throws IOException
	 */
	@Test
	public void testGetExport() throws IOException {
		Request request = request("/export")
			.get()
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test creating an export
	 * @throws IOException
	 */
	@Test
	public void testCreateExport() throws IOException {
		Request request = request("/export")
			.post(DUMMY_REQUEST_BODY)
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test getting the binaries
	 * @throws IOException
	 */
	@Test
	public void testGetBinaries() throws IOException {
		Request request = request("/binaries")
			.get()
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test creating the binaries zip
	 * @throws IOException
	 */
	@Test
	public void testCreateBinariesZip() throws IOException {
		Request request = request("/binaries")
			.post(DUMMY_REQUEST_BODY)
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Test checking the download status
	 * @throws IOException
	 */
	@Test
	public void testCheckDownloadStatus() throws IOException {
		Request request = request("/download/export/status")
			.get()
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeCheckDownloadStatus);
	}

	/**
	 * Test getting the download
	 * @throws IOException
	 */
	@Test
	public void testGetDownload() throws IOException {
		Request request = request("/download/%s".formatted(uuidSupplier.get()))
			.get()
			.build();
		Response response = client().newCall(request).execute();
		assertResponse(response, expectedResponseCodeLoad);
	}

	/**
	 * Create a request builder for a form request (no subpath)
	 * @return request builder
	 */
	protected Request.Builder request() {
		return request("");
	}

	/**
	 * Create a request builder for form request with the given subpath (should start with /)
	 * @param subPath subpath
	 * @return request builder
	 */
	protected Request.Builder request(String subPath) {
		return new Request.Builder()
				.url(HttpUrl.get(projectUrl("/forms/%s%s".formatted(uuidSupplier.get(), subPath))).newBuilder().build())
				.header("Referer", "ref").headers(auth(true));
	}
}
