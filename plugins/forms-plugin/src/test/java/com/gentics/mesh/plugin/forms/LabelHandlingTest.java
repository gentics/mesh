package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.plugin.common.TemplateWrapper;
import com.gentics.mesh.plugin.common.service.RenderService;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.dagger.FormsPluginModule;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.FormElement;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Test the label handling of form elements, using the form "label_handling.json", which contains
 * <ul>
 * <li>an element with a label only defined in the "schema" element</li>
 * <li>an element with a label only defined in the "uiSchema" element</li>
 * <li>an element with labels defined in the "schema" and "uiSchema" elements</li>
 * <li>an element with a label defined in the "schema" element and empty translations in the "uiSchema" element</li>
 * <li>an element with a label defined in the "schema" element and partially empty translations in the "uiSchema" element</li>
 * <li>a single value select element</li>
 * <li>a multi value (list) select element</li>
 * </ul>
 * The test is run for the languages "en" and "de".
 */
@RunWith(Parameterized.class)
public class LabelHandlingTest {
	public static final String SCHEMA_LABEL_ELEMENT = "input_schema_label";

	public static final String UI_LABEL_ELEMENT = "input_ui_label";

	public static final String BOTH_LABELS_ELEMENT = "input_both_labels";

	public static final String EMPTY_UI_LABEL_ELEMENT = "input_empty_ui_label";

	public static final String PARTIAL_UI_LABEL_ELEMENT = "input_partial_ui_label";

	public static final String SINGLE_SELECT_ELEMENT = "select_single";

	public static final String MULTI_SELECT_ELEMENT = "select_multi";

	public static final String SCHEMA_LABEL_VALUE = "Value of schema label input";

	public static final String UI_LABEL_VALUE = "Value of ui label input";

	public static final String BOTH_LABELS_VALUE = "Value of both labels input";

	public static final String EMPTY_UI_LABEL_VALUE = "Value of empty ui label input";

	public static final String PARTIAL_UI_LABEL_VALUE = "Value of partial ui label input";

	private static Vertx vertx;

	private static ObjectMapper objectMapper;

	private static RenderService renderService;

	/**
	 * Language used to resolve labels
	 */
	@Parameter(0)
	public String language;

	/**
	 * Expected ui-schema label of the input element
	 */
	@Parameter(1)
	public String expectedUiInputLabel;

	/**
	 * Expected ui-schema label of the single select element
	 */
	@Parameter(2)
	public String expectedUiSingleSelectLabel;

	/**
	 * Expected ui-schema label of the multi select element
	 */
	@Parameter(3)
	public String expectedUiMultiSelectLabel;

	/**
	 * Expected label of the selected option of the single select element
	 */
	@Parameter(4)
	public String expectedSingleValueLabel;

	/**
	 * Expected labels of the selected options of the multi select element
	 */
	@Parameter(5)
	public String[] expectedMultiValueLabels;

	/**
	 * Expected ui-schema label of the input element, which also has a schema label
	 */
	@Parameter(6)
	public String expectedBothLabelsLabel;

	private Form form;

	private NodeResponse formDataNode;

	/**
	 * Get the test parameters
	 * @return test parameters
	 */
	@Parameters(name = "{index}: language {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(
			new Object[] { "en", "UI label of input", "UI label of single select", "UI label of multi select",
					"Single option 2", new String[] { "Multi option 1", "Multi option 3" }, "Both labels: UI label" },
			new Object[] { "de", "UI-Label der Eingabe", "UI-Label der Einzelwahl", "UI-Label der Mehrfachwahl",
					"Einzelwahl 2", new String[] { "Mehrfachwahl 1", "Mehrfachwahl 3" }, "Beide Labels: UI-Label" });
	}

	@BeforeClass
	public static void setupOnce() throws URISyntaxException {
		vertx = Vertx.vertx();

		FormsConfig config = new FormsConfig();
		config.setTemplatesPath(new File(LabelHandlingTest.class.getResource("/templates").toURI()).getPath());

		FormsPluginModule module = new FormsPluginModule();
		objectMapper = module.provideObjectMapper(config);
		renderService = module.renderService(vertx, config);
	}

	@AfterClass
	public static void tearDown() {
		vertx.close();
	}

	/**
	 * Load the form and prepare the posted form data
	 * @throws IOException
	 */
	@Before
	public void setup() throws IOException {
		form = objectMapper.readValue(
				IOUtils.toString(LabelHandlingTest.class.getResource("label_handling.json"), StandardCharsets.UTF_8),
				Form.class);

		formDataNode = new NodeResponse();
		formDataNode.setUuid("data-uuid");
		formDataNode.setFields(new FieldMapImpl());
		formDataNode.getFields().put(SCHEMA_LABEL_ELEMENT, new StringFieldImpl().setString(SCHEMA_LABEL_VALUE));
		formDataNode.getFields().put(UI_LABEL_ELEMENT, new StringFieldImpl().setString(UI_LABEL_VALUE));
		formDataNode.getFields().put(BOTH_LABELS_ELEMENT, new StringFieldImpl().setString(BOTH_LABELS_VALUE));
		formDataNode.getFields().put(EMPTY_UI_LABEL_ELEMENT, new StringFieldImpl().setString(EMPTY_UI_LABEL_VALUE));
		formDataNode.getFields().put(PARTIAL_UI_LABEL_ELEMENT, new StringFieldImpl().setString(PARTIAL_UI_LABEL_VALUE));
		formDataNode.getFields().put(SINGLE_SELECT_ELEMENT, new StringFieldImpl().setString("single_2"));
		formDataNode.getFields().put(MULTI_SELECT_ELEMENT,
				new StringFieldListImpl().setItems(Arrays.asList("multi_1", "multi_3")));
	}

	/**
	 * Test the label of an element, which has the label only defined in the "schema" element
	 */
	@Test
	public void testLabelOnlyInSchema() {
		JsonObject data = getReportableData(SCHEMA_LABEL_ELEMENT);

		assertThat(data.getString("label")).as("Label in reportable data").isEqualTo("Schema only label");
		assertThat(data.getString("value")).as("Value in reportable data").isEqualTo(SCHEMA_LABEL_VALUE);
		assertThat(data.containsKey("valueLabel")).as("Reportable data contains value label").isFalse();

		assertThat(getRow(renderAdminMail(), "Schema only label")).as("Rendered row in admin mail")
				.contains(SCHEMA_LABEL_VALUE);
	}

	/**
	 * Test the label of an element, which has the label only defined in the "uiSchema" element
	 */
	@Test
	public void testLabelOnlyInUiSchema() {
		assertThat(getElement(UI_LABEL_ELEMENT).getLabel(language, form.getUiSchema())).as("Element label")
				.isEqualTo(expectedUiInputLabel);

		assertInputLabel(UI_LABEL_ELEMENT, expectedUiInputLabel, UI_LABEL_VALUE);
	}

	/**
	 * Test the label of an element, which has labels defined in the "schema" and the "uiSchema" element.
	 * The label from the "uiSchema" element must take precedence over the label from the "schema" element
	 */
	@Test
	public void testUiSchemaLabelTakesPrecedence() {
		assertThat(getElement(BOTH_LABELS_ELEMENT).getLabel(language, form.getUiSchema())).as("Element label")
				.isEqualTo(expectedBothLabelsLabel);

		String mail = assertInputLabel(BOTH_LABELS_ELEMENT, expectedBothLabelsLabel, BOTH_LABELS_VALUE);
		assertThat(mail).as("Rendered admin mail").doesNotContain("Both labels: schema label");
	}

	/**
	 * Test the label of an element, which has a label defined in the "schema" element and only empty (or blank)
	 * translations in the "uiSchema" element. The empty translations must be ignored, so the label from the
	 * "schema" element is used
	 */
	@Test
	public void testEmptyUiSchemaLabelIsIgnored() {
		assertThat(getElement(EMPTY_UI_LABEL_ELEMENT).getUiSchemaLabel(language, form.getUiSchema()))
				.as("UI schema label").isEmpty();
		assertThat(getElement(EMPTY_UI_LABEL_ELEMENT).getLabel(language, form.getUiSchema())).as("Element label")
				.isEqualTo(EMPTY_UI_LABEL_ELEMENT);

		assertInputLabel(EMPTY_UI_LABEL_ELEMENT, "Empty UI label: schema label", EMPTY_UI_LABEL_VALUE);
	}

	/**
	 * Test the label of an element, which has a label defined in the "schema" element and an empty translation
	 * for "en" in the "uiSchema" element. The empty translation must be ignored, so the first non-empty
	 * translation of the "uiSchema" element is used for all languages
	 */
	@Test
	public void testPartiallyEmptyUiSchemaLabel() {
		assertThat(getElement(PARTIAL_UI_LABEL_ELEMENT).getLabel(language, form.getUiSchema())).as("Element label")
				.isEqualTo("Teilweise leeres UI-Label");

		String mail = assertInputLabel(PARTIAL_UI_LABEL_ELEMENT, "Teilweise leeres UI-Label", PARTIAL_UI_LABEL_VALUE);
		assertThat(mail).as("Rendered admin mail").doesNotContain("Partial UI label: schema label");
	}

	/**
	 * Test the labels of a single value select element
	 */
	@Test
	public void testSingleSelectLabels() {
		assertThat(getElement(SINGLE_SELECT_ELEMENT).getLabel(language, form.getUiSchema())).as("Element label")
				.isEqualTo(expectedUiSingleSelectLabel);

		JsonObject data = getReportableData(SINGLE_SELECT_ELEMENT);
		assertThat(data.getString("label")).as("Label in reportable data").isEqualTo(expectedUiSingleSelectLabel);
		assertThat(data.getString("value")).as("Value in reportable data").isEqualTo("single_2");
		assertThat(data.getValue("valueLabel")).as("Value label in reportable data")
				.isEqualTo(expectedSingleValueLabel);

		assertThat(getRow(renderAdminMail(), expectedUiSingleSelectLabel)).as("Rendered row in admin mail")
				.contains(expectedSingleValueLabel)
				.doesNotContain("<li>");
	}

	/**
	 * Test the labels of a multi value select element
	 */
	@Test
	public void testMultiSelectLabels() {
		assertThat(getElement(MULTI_SELECT_ELEMENT).getLabel(language, form.getUiSchema())).as("Element label")
				.isEqualTo(expectedUiMultiSelectLabel);

		JsonObject data = getReportableData(MULTI_SELECT_ELEMENT);
		assertThat(data.getString("label")).as("Label in reportable data").isEqualTo(expectedUiMultiSelectLabel);
		assertThat(data.getJsonArray("value")).as("Value in reportable data")
				.containsExactly("multi_1", "multi_3");
		assertThat(data.getValue("valueLabel")).as("Value label in reportable data").isInstanceOf(JsonArray.class);
		assertThat(data.getJsonArray("valueLabel")).as("Value label in reportable data")
				.containsExactly((Object[]) expectedMultiValueLabels);

		StringBuilder expectedList = new StringBuilder();
		for (String label : expectedMultiValueLabels) {
			expectedList.append("<li>").append(label).append("</li>");
		}
		assertThat(getRow(renderAdminMail(), expectedUiMultiSelectLabel)).as("Rendered row in admin mail")
				.contains("<ul>")
				.contains(expectedList.toString());
	}

	/**
	 * Assert that the reportable data of the given input element contains the expected label and value
	 * (and no value label), and that the admin mail renders the value in the row of the expected label
	 * @param name element name
	 * @param expectedLabel expected label
	 * @param expectedValue expected value
	 * @return rendered admin mail
	 */
	protected String assertInputLabel(String name, String expectedLabel, String expectedValue) {
		JsonObject data = getReportableData(name);
		assertThat(data.getString("label")).as("Label in reportable data").isEqualTo(expectedLabel);
		assertThat(data.getString("value")).as("Value in reportable data").isEqualTo(expectedValue);
		assertThat(data.containsKey("valueLabel")).as("Reportable data contains value label").isFalse();

		String mail = renderAdminMail();
		assertThat(getRow(mail, expectedLabel)).as("Rendered row in admin mail").contains(expectedValue);
		return mail;
	}

	/**
	 * Get the form element with given name
	 * @param name element name
	 * @return form element
	 */
	protected FormElement getElement(String name) {
		return form.getSchema().getProperties().values().stream().filter(e -> name.equals(e.getName())).findFirst()
				.orElseThrow(() -> new AssertionError("Element " + name + " not found"));
	}

	/**
	 * Get the reportable data of the element with given name
	 * @param name element name
	 * @return reportable data of the element
	 */
	protected JsonObject getReportableData(String name) {
		JsonArray data = form.getReportableFormData(formDataNode, FormElement::isActive, language);
		return data.stream().map(JsonObject.class::cast).filter(o -> name.equals(o.getString("name"))).findFirst()
				.orElseThrow(() -> new AssertionError("No reportable data found for " + name + " in " + data.encodePrettily()));
	}

	/**
	 * Render the admin mail (with the same context as built in MailService.sendMail())
	 * @return rendered admin mail
	 */
	protected String renderAdminMail() {
		Map<String, Object> ctx = new HashMap<>();
		ctx.put("baseUrl", "http://localhost");
		ctx.put("form", new JsonObject(form.removeInactiveElements().toJson()));
		ctx.put("data", form.getReportableFormData(formDataNode, FormElement::isActive, language));
		ctx.put("dataUuid", formDataNode.getUuid());

		return renderService.rxRender(ctx, TemplateWrapper.wrap(null, "adminemail")).blockingGet().toString();
	}

	/**
	 * Get the part of the rendered mail between the given element label and the end of its table row.
	 * @param content rendered mail
	 * @param label element label
	 * @return rendered row content after the label
	 */
	protected String getRow(String content, String label) {
		assertThat(content).as("Rendered mail").contains(label);
		return StringUtils.substringBefore(StringUtils.substringAfter(content, label), "</tr>");
	}
}
