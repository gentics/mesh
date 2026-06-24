package com.gentics.mesh.plugin.forms;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.forms.model.schema.FormData;
import com.gentics.mesh.plugin.forms.model.schema.FormDataProperty;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
		Response response = client().newCall(request(null, requestBody("number", "11"))).execute();

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
		Response response = client().newCall(request(null, requestBody("number", "25"))).execute();

		assertValidationResponse(response, Map.of("number", "The value 25 is too large."));
		assertThat(getFormDataNodes(formUuid)).as("Form Data Nodes").hasSize(1);
	}

	/**
	 * Test validation of new form data with regex validation errors.
	 *
	 * @throws IOException
	 */
	@Test
	public void testValidateNewDataWithRegexErrors() throws IOException {
		Response response = client().newCall(request(null, requestBody("pattern", "invalidvalue"))).execute();

		assertValidationResponse(response, Map.of("pattern", "The value invalidvalue does not match the expected pattern."));
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

		Response response = client().newCall(request(formDataUuid, requestBody("number", "11"))).execute();

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

		Response response = client().newCall(request(formDataUuid, requestBody("number", "3"))).execute();

		assertValidationResponse(response, Map.of("number", "The value 3 is too small."));
		assertThat(getFormDataNodes(formUuid))
			.as("Form Data Nodes")
			.extracting(NodeResponse::getUuid)
			.containsExactlyElementsOf(dataNodesBeforeValidation.stream().map(NodeResponse::getUuid).toList());
	}

	@Test
	public void testInvalidEmailAddress() throws IOException {
		Response response = client().newCall(request(null, requestBody("usermail1", "invalid_email_address"))).execute();

		assertValidationResponse(response, Map.of("usermail1", "The email address invalid_email_address is invalid."));
	}

	@Test
	public void testEmptyMandatoryField() throws IOException {
		Response response = client().newCall(request(null,
				RequestBody.create(
						AbstractFormsPluginIntegrationTest.makeFormdataMultipart(formUuid, null, Map.of("input1", "")),
						MediaType.get("application/json"))))
				.execute();

		assertValidationResponse(response, Map.of("input1", "The field Input 1 is mandatory."));
	}

	/**
	 * An empty mandatory field that is explicitly marked as not visible (visible=false) must not
	 * produce a validation error, so a hidden/conditional field cannot block the form.
	 */
	@Test
	public void testHiddenMandatoryFieldSkipsValidation() throws IOException {
		Response response = client().newCall(request(null, bodyWithVisibility("input1", "", false))).execute();

		assertResponse(response, HttpResponseStatus.OK.code());
		var json = new JsonObject(response.body().string());
		assertThat(json.getBoolean("hasValidationErrors")).as("hasValidationErrors").isFalse();
		assertThat(json.getJsonObject("properties").getJsonObject("input1").getString("validationMessage"))
			.as("validationMessage of hidden field").isNull();
	}

	/**
	 * A visible field (visible=true or undefined) must still be validated. Only an explicit visible=false skips
	 * validation - the mere presence of the flag must not disable it.
	 */
	@Test
	public void testVisibleMandatoryFieldStillValidates() throws IOException {
		Response response = client().newCall(request(null, bodyWithVisibility("input1", "", null))).execute();

		assertValidationResponse(response, Map.of("input1", "The field Input 1 is mandatory."));
	}

	/**
	 * Build a validate request body containing a single property with an explicit {@code visible} flag.
	 * The shared {@link AbstractFormsPluginIntegrationTest#makeFormdataMultipart} helper only sets the
	 * value, so the body is built directly to be able to set {@code visible}.
	 */
	private RequestBody bodyWithVisibility(String field, String value, Boolean visible) {
		FormData data = new FormData()
			.setFormID(formUuid)
			.setProperties(Map.of(field, new FormDataProperty().setValue(value).setVisible(visible)));
		return RequestBody.create(JsonUtil.toJson(data), MediaType.get("application/json"));
	}

	private Request request(String formDataUuid, RequestBody requestBody) {
		HttpUrl.Builder url = HttpUrl.get(projectUrl("/form/%s/validate?lang=en".formatted(formUuid))).newBuilder();

		if (formDataUuid != null) {
			url.addQueryParameter("formDataUuid", formDataUuid);
		}

		return new Request.Builder()
			.url(url.build())
			.header("Referer", "ref")
			.header("Content-Type", "application/json")
			.post(requestBody)
			.build();
	}

	private RequestBody requestBody(String name, String value) {
		return RequestBody.create(AbstractFormsPluginIntegrationTest.makeFormdataMultipart(formUuid, formDataUuid,
				Map.of("input1", "bla", name, value)), MediaType.get("application/json"));
	}

	private void assertValidationResponse(Response response, Map<String, String> expectedValidationErrors) throws IOException {
		assertResponse(response, HttpResponseStatus.OK.code());

		var json = new JsonObject(response.body().string());

		assertThat(json.getBoolean("hasValidationErrors"))
			.as("hasValidationErrors")
			.isEqualTo(!expectedValidationErrors.isEmpty());

		System.out.println(json.encodePrettily());

		var invalidProperties = json.getJsonObject("properties");

		assertThat(invalidProperties)
			.as("Invalid Properties")
			.isNotNull()
			.isNotEmpty();

		for (var entry: expectedValidationErrors.entrySet()) {
			assertThat(invalidProperties.getJsonObject(entry.getKey()).getString("validationMessage"))
					.as("Invalid Property %s".formatted(entry.getKey()))
					.isEqualTo(entry.getValue());
		}
	}
}
