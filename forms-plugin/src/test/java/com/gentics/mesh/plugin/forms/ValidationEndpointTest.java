package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;

import io.vertx.core.json.JsonObject;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Test cases for validating form data without saving it.
 */
public class ValidationEndpointTest extends AbstractEndpointValidationTest {
	/**
	 * Test validation of new form data without validation errors.
	 *
	 * @throws IOException
	 */
	@Test
	public void testValidateNewDataWithoutErrors() throws IOException {
		Response response = client().newCall(request(null, requestBody("11"))).execute();

		assertValidationResponse(response, Collections.emptyMap());
		assertThat(getFormDataNodes(formUuid)).as("Form Data Nodes").hasSize(1);
	}

	/**
	 * Test validation of new form data with validation errors.
	 *
	 * @throws IOException
	 */
	@Test
	public void testValidateNewDataWithErrors() throws IOException {
		Response response = client().newCall(request(null, requestBody("25"))).execute();

		assertValidationResponse(response, Map.of("number", "Der Wert 25 ist ungültig."));
		assertThat(getFormDataNodes(formUuid)).as("Form Data Nodes").hasSize(1);
	}

	/**
	 * Test validation of new form data with regex validation errors.
	 *
	 * @throws IOException
	 */
	@Test
	public void testValidateNewDataWithRegexErrors() throws IOException {
		Response response = client().newCall(request(null, requestBody("OOOO"))).execute();

		assertValidationResponse(response, Map.of("number", "Does not fit regex"));
		assertThat(getFormDataNodes(formUuid)).as("Form Data Nodes").hasSize(1);
	}

	/**
	 * Test validation of existing form data without validation errors.
	 *
	 * @throws IOException
	 */
	@Test
	public void testValidateExistingDataWithoutErrors() throws IOException {
		List<NodeResponse> dataNodesBeforeValidation = getFormDataNodes(formUuid);

		Response response = client().newCall(request(formDataUuid, requestBody("11"))).execute();

		assertValidationResponse(response, Collections.emptyMap());
		assertThat(getFormDataNodes(formUuid))
			.as("Form Data Nodes")
			.extracting(NodeResponse::getUuid)
			.containsExactlyElementsOf(dataNodesBeforeValidation.stream().map(NodeResponse::getUuid).toList());
	}

	/**
	 * Test validation of existing form data with validation errors.
	 *
	 * @throws IOException
	 */
	@Test
	public void testValidateExistingDataWithErrors() throws IOException {
		List<NodeResponse> dataNodesBeforeValidation = getFormDataNodes(formUuid);

		Response response = client().newCall(request(formDataUuid, requestBody("3"))).execute();

		assertValidationResponse(response, Map.of("number", "Too small"));
		assertThat(getFormDataNodes(formUuid))
			.as("Form Data Nodes")
			.extracting(NodeResponse::getUuid)
			.containsExactlyElementsOf(dataNodesBeforeValidation.stream().map(NodeResponse::getUuid).toList());
	}

	private Request request(String formDataUuid, RequestBody requestBody) {
		HttpUrl.Builder url = HttpUrl.get(projectUrl("/form/%s/validate?lang=en".formatted(formUuid))).newBuilder();

		if (formDataUuid != null) {
			url.addQueryParameter("formDataUuid", formDataUuid);
		}

		return new Request.Builder()
			.url(url.build())
			.header("Referer", "ref")
			.post(requestBody)
			.build();
	}

	private RequestBody requestBody(String value) {
		return new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("formdata", AbstractFormsPluginIntegrationTest.makeFormdataMultipart(formUuid, formDataUuid, Map.of("input1", "bla", "number", value)))
			.build();
	}

	private void assertValidationResponse(Response response, Map<String, String> expectedValidationErrors) throws IOException {
		assertResponse(response, 200);

		JsonObject json = new JsonObject(response.body().string());

		assertThat(json.getBoolean("hasValidationErrors"))
			.as("hasValidationErrors")
			.isEqualTo(!expectedValidationErrors.isEmpty());

		System.out.println(json.encodePrettily());

		for (var entry: expectedValidationErrors.entrySet()) {
		}
	}
}
