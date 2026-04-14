package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.model.schema.FormData;
import com.gentics.mesh.plugin.forms.model.schema.FormUiSchema;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying that {@code schema}, {@code uiSchema} and {@code data}
 * are correctly deserialized from the new form JSON format.
 */
public class FormDeserializationTest {

	private ObjectMapper objectMapper;

	@Before
	public void setUp() {
		objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		SimpleModule module = new SimpleModule();
		module.addDeserializer(FormElement.class, new FormElementDeserializer<>(new FormsConfig()));
		objectMapper.registerModule(module);
	}

	private String loadJson() throws Exception {
		InputStream in = getClass().getResourceAsStream("form-deserialization.json");
		assertThat(in).as("test fixture found").isNotNull();
		return new String(in.readAllBytes());
	}

	private Form loadForm() throws Exception {
		return objectMapper.readValue(loadJson(), Form.class);
	}

	// ── schema ───────────────────────────────────────────────────────────────

	@Test
	public void schemaIsDeserialized() throws Exception {
		Form form = loadForm();
		assertThat(form.getSchema()).as("schema").isNotNull();
		assertThat(form.getSchema().getKey()).as("schema.key").isEqualTo("test-schema");
		assertThat(form.getSchema().getVersion()).as("schema.version").isEqualTo("1.0.0");
	}

	 @Test
	 public void schemaPropertiesContainAllControls() throws Exception {
	 	Form form = loadForm();
	 	assertThat(form.getSchema().getProperties())
	 		.as("schema.properties keys")
	 		.containsKeys("firstName", "appointmentDate", "topic", "interests", "accepted");
	 }

	 @Test
	 public void stringElementIsDeserialized() throws Exception {
	 	Form form = loadForm();
	 	FormElement firstName = form.getSchema().getProperties().get("firstName");
	 	assertThat(firstName).as("firstName element").isInstanceOf(InputElement.class);
	 	assertThat(firstName.isMandatory()).as("firstName mandatory").isTrue();
	 	assertThat(firstName.isList()).as("firstName isList").isFalse();
	 }

	 @Test
	 public void datetimeElementWithDateOnlyIsDeserialized() throws Exception {
	 	Form form = loadForm();
	 	FormElement appointment = form.getSchema().getProperties().get("appointmentDate");
	 	assertThat(appointment).as("appointmentDate element").isInstanceOf(DatetimeElement.class);
	 	assertThat(((DatetimeElement) appointment).isDateOnly()).as("dateOnly").isTrue();
	 }

	 @Test
	 public void selectElementIsDeserialized() throws Exception {
	 	Form form = loadForm();
	 	FormElement topic = form.getSchema().getProperties().get("topic");
	 	assertThat(topic).as("topic element").isInstanceOf(DropDownElement.class);
	 }

	 @Test
	 public void booleanElementMapsToCheckbox() throws Exception {
	 	Form form = loadForm();
	 	FormElement accepted = form.getSchema().getProperties().get("accepted");
	 	assertThat(accepted).as("accepted element").isInstanceOf(BooleanElement.class);
	 }

	 @Test
	 public void isListFlagIsDeserialized() throws Exception {
	 	Form form = loadForm();
	 	assertThat(form.getSchema().getProperties().get("interests").isList())
	 		.as("interests isList").isTrue();
	 	assertThat(form.getSchema().getProperties().get("firstName").isList())
	 		.as("firstName isList").isFalse();
	 }

	 // ── uiSchema ─────────────────────────────────────────────────────────────

	 @Test
	 public void uiSchemaIsDeserialized() throws Exception {
	 	Form form = loadForm();
	 	FormUiSchema ui = form.getUiSchema();
	 	assertThat(ui).as("uiSchema").isNotNull();
	 	assertThat(ui.getKey()).as("uiSchema.key").isEqualTo("test-ui");
	 	assertThat(ui.getVersion()).as("uiSchema.version").isEqualTo("1.0.0");
	 }

	 @Test
	 public void uiSchemaFormnameIsDeserialized() throws Exception {
	 	FormUiSchema ui = loadForm().getUiSchema();
	 	assertThat(ui.getFormname()).as("uiSchema.formname")
	 		.containsEntry("de", "Testformular")
	 		.containsEntry("en", "Test Form");
	 }

	 @Test
	 public void uiSchemaPagesArePreserved() throws Exception {
	 	FormUiSchema ui = loadForm().getUiSchema();
	 	assertThat(ui.getPages()).as("uiSchema.pages").isNotNull().hasSize(1);
	 }

	 // ── data ─────────────────────────────────────────────────────────────────

	 @Test
	 public void dataIsDeserialized() throws Exception {
	 	Form form = loadForm();
	 	FormData data = form.getData();
	 	assertThat(data).as("data").isNotNull();
	 	assertThat(data.getFormID()).as("data.formID")
	 		.isEqualTo("83C9CFC9-F7ED-4EB8-A3F2-9F8F53E048A9");
	 	assertThat(data.isHasValidationErrors()).as("data.hasValidationErrors").isFalse();
	 }

	 @Test
	 public void dataPropertiesSingleValueIsDeserialized() throws Exception {
	 	FormData data = loadForm().getData();
	 	assertThat(data.getProperties().get("firstName").getValue())
	 		.as("data.properties.firstName.value").isEqualTo("Anna");
	 }

	 @Test
	 public void dataPropertiesMultiValueIsDeserialized() throws Exception {
	 	FormData data = loadForm().getData();
	 	assertThat(data.getProperties().get("interests").getValues())
	 		.as("data.properties.interests.values")
	 		.containsExactly("general", "support");
	 }

	 @Test
	 public void dataPropertiesOptionalFieldsAreDeserialized() throws Exception {
	 	FormData data = loadForm().getData();
	 	var firstName = data.getProperties().get("firstName");
	 	assertThat(firstName.getVisible()).as("visible").isTrue();
	 	assertThat(firstName.getEditable()).as("editable").isTrue();

	 	var accepted = data.getProperties().get("accepted");
	 	assertThat(accepted.getValidationMessage()).as("validationMessage").isEqualTo("Pflichtfeld");
	 }

	// // ── round-trip ───────────────────────────────────────────────────────────

	@Test
	public void roundTripPreservesFormContent() throws Exception {
		String originalJson = loadJson();
		Form form = objectMapper.readValue(originalJson, Form.class);
		JsonNode original = objectMapper.readTree(originalJson);
		JsonNode reserialized = objectMapper.readTree(objectMapper.writeValueAsString(form));

		// Root-level scalar properties
		List<String> rootFields = Arrays.asList(
			"name", "language", "type", "successurl",
			"email", "mailsource", "templateContext", "downloadBaseUrl"
		);
		for (String field : rootFields) {
			assertThat(reserialized.get(field))
				.as("root field '%s'", field)
				.isEqualTo(original.get(field));
		}

		// schema metadata (key/version preserved; properties intentionally differ after deserialization)
		assertThat(reserialized.get("schema").get("key")).as("schema.key")
			.isEqualTo(original.get("schema").get("key"));
		assertThat(reserialized.get("schema").get("version")).as("schema.version")
			.isEqualTo(original.get("schema").get("version"));

		

		// uiSchema and data are pure pass-through — full tree must match
		assertThat(reserialized.get("schema")).as("schema").isEqualTo(original.get("schema"));
		assertThat(reserialized.get("uiSchema")).as("uiSchema").isEqualTo(original.get("uiSchema"));
		assertThat(reserialized.get("data")).as("data").isEqualTo(original.get("data"));
	}
}
