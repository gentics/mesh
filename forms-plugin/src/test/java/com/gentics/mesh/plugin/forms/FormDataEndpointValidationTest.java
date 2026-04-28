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
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
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
	 * Test submitting (updating + publishing) form data
	 * @throws IOException
	 */
	@Test
	public void testSubmitData() throws IOException {
		Request postRequest = requestSubmit().post(updateRequestBody()).build();
		Response response = client().newCall(postRequest).execute();
		assertResponse(response, expectedResponseCode);
	}

	/**
	 * Test saving (updating without publishing) form data
	 * @throws IOException
	 */
	@Test
	public void testSaveData() throws IOException {
		Request postRequest = requestSave().post(updateRequestBody()).build();
		Response response = client().newCall(postRequest).execute();
		assertResponse(response, expectedResponseCode);
	}

	/**
	 * Test deleting form data
	 * @throws IOException
	 */
	@Test
	public void testDeleteData() throws IOException {
		Request deleteRequest = request().delete().build();
		Response response = client().newCall(deleteRequest).execute();
		assertResponse(response, expectedResponseCode);
	}

	protected Request.Builder request() {
		return new Request.Builder()
				.url(HttpUrl.get(projectUrl("/forms/%s/data/%s".formatted(formUuidSupplier.get(),
						formDataUuidSupplier.get()))).newBuilder().build())
				.header("Referer", "ref");
	}

	protected Request.Builder requestSubmit() {
		return new Request.Builder()
				.url(HttpUrl.get(projectUrl("/forms/%s/submit".formatted(formUuidSupplier.get()))).newBuilder().build())
				.header("Referer", "ref");
	}

	protected Request.Builder requestSave() {
		return new Request.Builder()
				.url(HttpUrl.get(projectUrl("/form/%s/save".formatted(formUuidSupplier.get()))).newBuilder().build())
				.header("Referer", "ref");
	}

	protected RequestBody updateRequestBody() {
		return new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("formDataUuid", formDataUuidSupplier.get())
				.addFormDataPart("input1", "bla")
				.build();
	}
}
