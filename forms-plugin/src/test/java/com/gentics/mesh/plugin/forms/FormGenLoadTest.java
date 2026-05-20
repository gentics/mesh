package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.json.JsonUtil;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FormGenLoadTest extends AbstractFormsPluginIntegrationTest {

	private static final String FORM_ID = "test-form-instance-01";

	@Before
	public void setup() {
		super.setup();
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
	}
	
	@Test
	public void testLoadFormGenFormGridOptionsNotExposed() throws IOException {
		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, FORM_ID, "de", true);
		String responseBody = response.body() != null ? response.body().string() : "";
		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode body = JsonUtil.readValue(responseBody, JsonNode.class);

		JsonNode schemaProperties = body.get("schema").get("properties");
		schemaProperties.fieldNames().forEachRemaining(key -> {
			assertThat(schemaProperties.get(key).has("formGridOptions"))
					.as("formGridOptions not exposed in schema.properties." + key)
					.isFalse();
		});
	}

	@Test
	public void testLoadFormGenPossibleValuesDe() throws IOException {
		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, FORM_ID, "de", true);
		String responseBody = response.body() != null ? response.body().string() : "";

		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode body = JsonUtil.readValue(responseBody, JsonNode.class);
		JsonNode properties = body.get("data").get("properties");

		assertThat(properties.has("topic")).as("properties has topic").isTrue();
		JsonNode topicProp = properties.get("topic");
		assertThat(topicProp.has("possibleValues")).as("topic has possibleValues").isTrue();

		JsonNode possibleValues = topicProp.get("possibleValues");
		assertThat(possibleValues.size()).as("possibleValues count").isEqualTo(3);

		assertThat(possibleValues.get(0).get("identifier").asText()).as("first identifier").isEqualTo("general");
		assertThat(possibleValues.get(0).get("displayName").asText()).as("first displayName de").isEqualTo("Allgemein");

		assertThat(possibleValues.get(1).get("identifier").asText()).as("second identifier").isEqualTo("support");
		assertThat(possibleValues.get(1).get("displayName").asText()).as("second displayName de").isEqualTo("Support");

		assertThat(possibleValues.get(2).get("identifier").asText()).as("third identifier").isEqualTo("drama");
		assertThat(possibleValues.get(2).get("displayName").asText()).as("third displayName de").isEqualTo("Drama");
	}

	@Test
	public void testLoadFormGenPossibleValuesEn() throws IOException {
		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, FORM_ID, "en", true);
		String responseBody = response.body() != null ? response.body().string() : "";

		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode body = JsonUtil.readValue(responseBody, JsonNode.class);
		JsonNode possibleValues = body.get("data").get("properties").get("topic").get("possibleValues");

		assertThat(possibleValues.get(0).get("displayName").asText()).as("first displayName en").isEqualTo("General");
		assertThat(possibleValues.get(1).get("displayName").asText()).as("second displayName en").isEqualTo("Support");
	}

	@Test
	public void testLoadFormGenSelectOptionsDefaultValue() throws IOException {
		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, FORM_ID, "de", true);
		String responseBody = response.body() != null ? response.body().string() : "";

		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode adminmail = JsonUtil.readValue(responseBody, JsonNode.class)
				.get("data").get("properties").get("adminmail").get("value");

		assertThat(adminmail.get("identifier").asText()).as("adminmail.value").isEqualTo("Herr");
		assertThat(adminmail.get("displayName").asText()).as("adminmail.label de").isEqualTo("Herr");
	}

	@Test
	public void testLoadFormGenNoPossibleValuesForNonValueListElement() throws IOException {
		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, FORM_ID, "de", true);
		String responseBody = response.body() != null ? response.body().string() : "";
		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode body = JsonUtil.readValue(responseBody, JsonNode.class);
		JsonNode properties = body.get("data").get("properties");

		// string/number elements must not have possibleValues
		assertThat(properties.get("firstName").has("possibleValues")).as("firstName has no possibleValues").isFalse();
		assertThat(properties.get("age").has("possibleValues")).as("age has no possibleValues").isFalse();
	}

	@Test
	public void testLoadFormGenMissingFormId() throws IOException {
		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, "", "en", true);
		assertThat(response.code()).as("Response code for empty formID").isEqualTo(400);
	}

	@Test
	public void testLoadFormGenNullBody() throws IOException {
		String uuid = createForm("formgen_load.json");

		Request request = new Request.Builder()
				.url(projectUrl("/form/" + uuid + "/load?lang=en"))
				.headers(auth(true))
				.post(RequestBody.create(MediaType.parse("application/json"), "{}"))
				.build();

		Response response = client().newCall(request).execute();
		assertThat(response.code()).as("Response code for missing formID").isEqualTo(400);
	}

	@Test
	public void testLoadFormGenNoPermission() throws IOException {
		String uuid = createForm("formgen_load.json");
		Response response = doLoadRequest(uuid, FORM_ID, "en", false);
		assertForbidden(response);
	}

	@Test
	public void testLoadFormGenWithFormDataUuid() throws IOException {
		String formUuid = createForm("formgen_load.json");

		submit(formUuid, false, false, false, true, "en", "firstName", "Hans", "notes", "Wichtige Notiz");
		String formDataUuid = getSingleFormDataNode(formUuid).getUuid();

		Response response = doLoadRequest(formUuid, FORM_ID, "en", true, formDataUuid);
		String responseBody = response.body() != null ? response.body().string() : "";

		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode body = JsonUtil.readValue(responseBody, JsonNode.class);
		JsonNode properties = body.get("data").get("properties");

		assertThat(properties.get("firstName").get("value").asText()).as("firstName from submission").isEqualTo("Hans");
		assertThat(properties.get("notes").get("value").asText()).as("notes from submission")
				.isEqualTo("Wichtige Notiz");
		// age was not submitted → value field absent (NON_NULL serialization)
		assertThat(properties.get("age").has("value")).as("age value absent when not submitted").isFalse();
	}

	@Test
	public void testLoadFormGenSelectOptionsValueForAdminElement() throws IOException {
		String formUuid = createForm("formgen_load.json");

		submit(formUuid, false, false, false, true, "en",
				"firstName", "Hans",
				"age", "42",
				"notes", "Wichtige Notiz",
				"topic", "support",
				"topicMulti", "drama",
				"tags", "tag1",
				"tags", "tag2",
				"greeting", "Hallo",
				"adminmail", "Ms");
		String formDataUuid = getSingleFormDataNode(formUuid).getUuid();

		Response response = doLoadRequest(formUuid, FORM_ID, "en", true, formDataUuid);
		String responseBody = response.body() != null ? response.body().string() : "";

		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode adminmail = JsonUtil.readValue(responseBody, JsonNode.class)
				.get("data").get("properties").get("adminmail").get("value");

		assertThat(adminmail.get("identifier").asText()).as("adminmail.value").isEqualTo("Ms");
		assertThat(adminmail.get("displayName").asText()).as("adminmail.label en").isEqualTo("Ms");
	}

	@Test
	public void testLoadFormGenSelectOptionsValueFromSubmission() throws IOException {
		String formUuid = createForm("formgen_load.json");

		submit(formUuid, false, false, false, true, "en",
				"firstName", "Hans",
				"age", "42",
				"notes", "Wichtige Notiz",
				"topic", "support",
				"topicMulti", "drama",
				"tags", "tag1",
				"tags", "tag2",
				"greeting", "Hallo",
				"adminmail", "Ms");
		String formDataUuid = getSingleFormDataNode(formUuid).getUuid();

		Response response = doLoadRequest(formUuid, FORM_ID, "en", true, formDataUuid);
		String responseBody = response.body() != null ? response.body().string() : "";

		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode adminmail = JsonUtil.readValue(responseBody, JsonNode.class)
				.get("data").get("properties").get("adminmail").get("value");

		assertThat(adminmail.get("identifier").asText()).as("adminmail.value").isEqualTo("Ms");
		assertThat(adminmail.get("displayName").asText()).as("adminmail.label en").isEqualTo("Ms");
	}
	

	@Test
	public void testLoadFormGenCatalogDefaultSingle() throws IOException {
		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, FORM_ID, "de", true);
		String responseBody = response.body() != null ? response.body().string() : "";
		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode properties = JsonUtil.readValue(responseBody, JsonNode.class).get("data").get("properties");

		JsonNode topicValue = properties.get("topic").get("value");
		assertThat(topicValue).as("topic.value is present").isNotNull();
		assertThat(topicValue.get("identifier").asText()).as("topic.value.identifier").isEqualTo("general");
		assertThat(topicValue.get("displayName").asText()).as("topic.value.displayName de").isEqualTo("Allgemein");
	}

	@Test
	public void testLoadFormGenCatalogDefaultMulti() throws IOException {
 		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, FORM_ID, "en", true);
		String responseBody = response.body() != null ? response.body().string() : "";
		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode properties = JsonUtil.readValue(responseBody, JsonNode.class).get("data").get("properties");

		JsonNode values = properties.get("topicMulti").get("values");
		assertThat(values).as("topicMulti.values is present").isNotNull();
		assertThat(values.size()).as("topicMulti.values count").isEqualTo(2);
		assertThat(values.get(0).get("identifier").asText()).as("topicMulti first identifier").isEqualTo("general");
		assertThat(values.get(0).get("displayName").asText()).as("topicMulti first displayName en")
				.isEqualTo("General");
		assertThat(values.get(1).get("identifier").asText()).as("topicMulti second identifier").isEqualTo("support");
		assertThat(values.get(1).get("displayName").asText()).as("topicMulti second displayName en")
				.isEqualTo("Support");
	}

	@Test
	public void testLoadFormGenArrayDefault() throws IOException {
		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, FORM_ID, "de", true);
		String responseBody = response.body() != null ? response.body().string() : "";
		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode properties = JsonUtil.readValue(responseBody, JsonNode.class).get("data").get("properties");

		JsonNode values = properties.get("tags").get("values");
		assertThat(values).as("tags.values is present").isNotNull();
		assertThat(values.size()).as("tags.values count").isEqualTo(2);
		assertThat(values.get(0).asText()).as("tags first value").isEqualTo("tag1");
		assertThat(values.get(1).asText()).as("tags second value").isEqualTo("tag2");
		assertThat(properties.get("tags").has("value")).as("tags.value absent").isFalse();
	}

	@Test
	public void testLoadFormGenTranslationDefaultDe() throws IOException {
		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, FORM_ID, "de", true);
		String responseBody = response.body() != null ? response.body().string() : "";
		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode properties = JsonUtil.readValue(responseBody, JsonNode.class).get("data").get("properties");

		assertThat(properties.get("greeting").get("value").asText()).as("greeting value de").isEqualTo("Hallo");
	}

	@Test
	public void testLoadFormGenTranslationDefaultEn() throws IOException {
		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, FORM_ID, "en", true);
		String responseBody = response.body() != null ? response.body().string() : "";
		assertSuccessfulJsonResponse(response, responseBody);
		JsonNode properties = JsonUtil.readValue(responseBody, JsonNode.class).get("data").get("properties");

		assertThat(properties.get("greeting").get("value").asText()).as("greeting value en").isEqualTo("Hello");
	}

	@Test
	public void testLoadFormGenPublishedFormDataReturnsConflict() throws IOException {
		String formUuid = createForm("formgen_load.json");
		// publish = true
		submit(formUuid, true, "en", "firstName", "Hans");
		String formDataUuid = getSingleFormDataNode(formUuid).getUuid();

		Response response = doLoadRequest(formUuid, FORM_ID, "en", true, formDataUuid);
		assertThat(response.code()).as("Response code for published formDataUuid").isEqualTo(409);
	}

	@Test
	public void testAdminMailElementIdentifierMasking() throws IOException {
		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, FORM_ID, "en", true);
		String responseBody = response.body() != null ? response.body().string() : "";
		assertSuccessfulJsonResponse(response, responseBody);

		JsonNode possibleValues = JsonUtil.readValue(responseBody, JsonNode.class)
				.get("data").get("properties").get("adminmail").get("possibleValues");

		assertThat(possibleValues.get(0).get("identifier").asText())
				.as("adminmail option 0 identifier is masked").isEqualTo("Mr");
		assertThat(possibleValues.get(0).get("displayName").asText())
				.as("adminmail option 0 displayName").isEqualTo("Mr");
		assertThat(possibleValues.get(1).get("identifier").asText())
				.as("adminmail option 1 identifier is masked").isEqualTo("Ms");
		assertThat(possibleValues.get(1).get("displayName").asText())
				.as("adminmail option 1 displayName").isEqualTo("Ms");
	}

	@Test
	public void testLoadFormGenCaptchaConfig() throws IOException {
		String uuid = createForm("formgen_load.json");

		Response response = doLoadRequest(uuid, FORM_ID, "en", true);
		String responseBody = response.body() != null ? response.body().string() : "";
		assertSuccessfulJsonResponse(response, responseBody);

		JsonNode captchaConfig = JsonUtil.readValue(responseBody, JsonNode.class).get("captchaConfig");
		assertThat(captchaConfig).as("captchaConfig is present").isNotNull();
		assertThat(captchaConfig.get("provider").asText()).as("provider").isEqualTo("recaptcha");
		assertThat(captchaConfig.get("mode").asText()).as("mode").isEqualTo("background");
		assertThat(captchaConfig.get("sitekey").asText()).as("sitekey").isEqualTo("your-site-key");
	}

	@Test
	public void testLoadFormGenCaptchaIdWithoutSiteKeyThrows() throws IOException {
		// friendlycaptcha has no siteKey in test config → expect 400
		String uuid = createForm("formgen_load_captcha_nositekey.json");

		Response response = doLoadRequest(uuid, FORM_ID, "en", true);
		assertThat(response.code()).as("Response code when captchaId set but sitekey missing").isEqualTo(400);
	}

	private Response doLoadRequest(String formUuid, String formID, String language, boolean permission)
			throws IOException {
		return doLoadRequest(formUuid, formID, language, permission, null);
	}

	private Response doLoadRequest(String formUuid, String formID, String language, boolean permission,
			String formDataUuid) throws IOException {
		String bodyJson = "{\"formID\": \"" + formID + "\"}";
		String url = projectUrl("/form/" + formUuid + "/load");
		if (language != null) {
			url = url + "?lang=" + language;
		}
		if (formDataUuid != null) {
			url = url + "&formDataUuid=" + formDataUuid;
		}
		return client().newCall(new Request.Builder()
				.url(url)
				.headers(auth(permission))
				.post(RequestBody.create(MediaType.parse("application/json"), bodyJson))
				.build()).execute();
	}

	private void assertSuccessfulJsonResponse(Response response, String responseBody) {
		assertThat(response.isSuccessful())
				.as("Response success %d (%s), body: %s", response.code(), response.message(), responseBody)
				.isTrue();
		assertThat(response.code()).as("Response code").isEqualTo(200);
		assertThat(response.header("Content-Type")).as("Content-Type").contains("application/json");
	}
}
