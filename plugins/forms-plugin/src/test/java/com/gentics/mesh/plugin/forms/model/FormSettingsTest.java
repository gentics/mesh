package com.gentics.mesh.plugin.forms.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gentics.mesh.plugin.forms.config.FormsConfig;

/**
 * Test cases for the custom settings ({@code formSettings}) of the {@link Form}
 */
public class FormSettingsTest {
	private static final String FORM_SETTINGS_JSON = "{\"stringSetting\":\"value\",\"numberSetting\":42,\"booleanSetting\":true,"
			+ "\"objectSetting\":{\"nested\":\"nestedValue\"},\"listSetting\":[\"one\",\"two\"]}";

	private static final String FORM_WITH_SETTINGS = "{\"name\":\"settings\",\"schema\":{\"properties\":{}},\"formSettings\":" + FORM_SETTINGS_JSON + "}";

	private static final String FORM_WITHOUT_SETTINGS = "{\"name\":\"settings\",\"schema\":{\"properties\":{}}}";

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

	/**
	 * Test that the settings are deserialized into the form settings and not into the additional properties
	 * @throws Exception
	 */
	@Test
	public void testDeserialize() throws Exception {
		Form form = objectMapper.readValue(FORM_WITH_SETTINGS, Form.class);

		Map<String, Object> settings = form.getFormSettings();
		assertThat(settings).as("Form settings").containsOnlyKeys("stringSetting", "numberSetting", "booleanSetting", "objectSetting",
				"listSetting");
		assertThat(settings).as("Form settings")
			.containsEntry("stringSetting", "value")
			.containsEntry("numberSetting", 42)
			.containsEntry("booleanSetting", true)
			.containsEntry("objectSetting", Map.of("nested", "nestedValue"))
			.containsEntry("listSetting", Arrays.asList("one", "two"));
		assertThat(form.getAny()).as("Additional properties").doesNotContainKey("formSettings");
	}

	/**
	 * Test that the settings are serialized unchanged
	 * @throws Exception
	 */
	@Test
	public void testRoundTrip() throws Exception {
		Form form = objectMapper.readValue(FORM_WITH_SETTINGS, Form.class);

		JsonNode serialized = objectMapper.readTree(objectMapper.writeValueAsString(form));
		assertThat(serialized.get("formSettings")).as("Serialized form settings").isEqualTo(objectMapper.readTree(FORM_SETTINGS_JSON));
	}

	/**
	 * Test that a form without settings returns an empty map and does not serialize the settings
	 * @throws Exception
	 */
	@Test
	public void testWithoutSettings() throws Exception {
		Form form = objectMapper.readValue(FORM_WITHOUT_SETTINGS, Form.class);

		assertThat(form.getFormSettings()).as("Form settings").isNotNull().isEmpty();

		JsonNode serialized = objectMapper.readTree(objectMapper.writeValueAsString(form));
		assertThat(serialized.has("formSettings")).as("Serialized form contains formSettings").isFalse();
	}

	/**
	 * Test that the copy created by {@link Form#removeInactiveElements()} contains the settings
	 * @throws Exception
	 */
	@Test
	public void testCopy() throws Exception {
		Form form = objectMapper.readValue(FORM_WITH_SETTINGS, Form.class);

		Form copy = form.removeInactiveElements();
		assertThat(copy.getFormSettings()).as("Form settings of the copy").isEqualTo(form.getFormSettings());
	}
}
