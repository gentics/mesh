package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
import io.vertx.core.json.JsonObject;

/**
 * Test rendering of the default mail templates "adminemail.hbs" and "useremail.hbs".
 */
public class MailTemplateTest {

	private static Vertx vertx;
	private static ObjectMapper objectMapper;
	private static RenderService renderService;

	@BeforeClass
	public static void setupOnce() throws URISyntaxException {
		vertx = Vertx.vertx();

		FormsConfig config = new FormsConfig();
		config.setTemplatesPath(new File(MailTemplateTest.class.getResource("/templates").toURI()).getPath());

		FormsPluginModule module = new FormsPluginModule();
		objectMapper = module.provideObjectMapper(config);
		renderService = module.renderService(vertx, config);
	}

	@AfterClass
	public static void tearDown() {
		vertx.close();
	}

	/**
	 * Test rendering of single and multi value selection elements with the admin mail template.
	 */
	@Test
	public void testAdminMailSelection() throws IOException {
		doTestSelection("adminemail");
	}

	/**
	 * Test rendering of single and multi value selection elements with the user mail template.
	 */
	@Test
	public void testUserMailSelection() throws IOException {
		doTestSelection("useremail");
	}

	/**
	 * Render the given template for data posted to the form "selection_with_mail.json" and check that only the
	 * multi value element is rendered as list, and that single value elements are rendered with their value (or
	 * label) instead of a list of the properties of the value.
	 *
	 * @param templateName The name of the mail template.
	 */
	private void doTestSelection(String templateName) throws IOException {
		Form form = objectMapper.readValue(
			IOUtils.toString(MailTemplateTest.class.getResource("selection_with_mail.json"), StandardCharsets.UTF_8),
			Form.class);

		NodeResponse formDataNode = new NodeResponse();
		formDataNode.setUuid("data-uuid");
		formDataNode.setFields(new FieldMapImpl());
		formDataNode.getFields().put("checkbox1", new StringFieldImpl().setString("X"));
		formDataNode.getFields().put("radiobuttons1", new StringFieldImpl().setString("r_2"));
		formDataNode.getFields().put("singleselect1", new StringFieldImpl().setString("s_1"));
		formDataNode.getFields().put("multiselect1", new StringFieldListImpl().setItems(Arrays.asList("m_1", "m_3")));

		// Same context as built in MailService.sendMail() (without language).
		Map<String, Object> ctx = new HashMap<>();
		ctx.put("baseUrl", "http://localhost");
		ctx.put("form", new JsonObject(form.removeInactiveElements().toJson()));
		ctx.put("data", form.getReportableFormData(formDataNode, e -> e.isActive(), null));
		ctx.put("dataUuid", formDataNode.getUuid());

		String content = renderService.rxRender(ctx, TemplateWrapper.wrap(null, templateName)).blockingGet().toString();

		assertThat(getRows(content)).as("Rendered rows in %s", templateName)
			.extracting(MailRow::label)
			.containsExactly("Checkbox element", "Radio buttons element", "Single select element", "Multi select element");

		assertSingleValueRow(templateName, content, "Checkbox element", "X");
		assertSingleValueRow(templateName, content, "Radio buttons element", "Radio button option 2");
		assertSingleValueRow(templateName, content, "Single select element", "Single-select option 1");

		assertThat(getRow(content, "Multi select element"))
			.as("Rendered row of multi-select element in %s", templateName)
			.contains("<ul>")
			.contains("<li>Multi-select option 1</li><li>Multi-select option 3</li>")
			.doesNotContain("false");
	}

	/**
	 * Test that the label placeholders of the admin mail template are filled with the resolved labels.
	 */
	@Test
	public void testAdminMailLabels() throws IOException {
		doTestLabels("adminemail", "en");
		doTestLabels("adminemail", "de");
	}

	/**
	 * Test that the label placeholders of the user mail template are filled with the resolved labels.
	 */
	@Test
	public void testUserMailLabels() throws IOException {
		doTestLabels("useremail", "en");
		doTestLabels("useremail", "de");
	}

	/**
	 * Render the given template for data posted to the form "label_handling.json" in the given language and check
	 * that every row contains the expected label (placeholder {@code label}) and the expected value (placeholders
	 * {@code valueLabel} or {@code value}). Labels from the "uiSchema" element take precedence over labels from the
	 * "schema" element, empty "uiSchema" labels are ignored.
	 *
	 * @param templateName The name of the mail template.
	 * @param language The language.
	 */
	private void doTestLabels(String templateName, String language) throws IOException {
		boolean de = "de".equals(language);
		Form form = objectMapper.readValue(
			IOUtils.toString(MailTemplateTest.class.getResource("label_handling.json"), StandardCharsets.UTF_8),
			Form.class);

		NodeResponse formDataNode = new NodeResponse();
		formDataNode.setUuid("data-uuid");
		formDataNode.setFields(new FieldMapImpl());
		formDataNode.getFields().put("input_schema_label", new StringFieldImpl().setString("value 1"));
		formDataNode.getFields().put("input_ui_label", new StringFieldImpl().setString("value 2"));
		formDataNode.getFields().put("input_both_labels", new StringFieldImpl().setString("value 3"));
		formDataNode.getFields().put("input_empty_ui_label", new StringFieldImpl().setString("value 4"));
		formDataNode.getFields().put("input_partial_ui_label", new StringFieldImpl().setString("value 5"));
		formDataNode.getFields().put("select_single", new StringFieldImpl().setString("single_2"));
		formDataNode.getFields().put("select_multi", new StringFieldListImpl().setItems(Arrays.asList("multi_1", "multi_3")));

		Map<String, Object> ctx = new HashMap<>();
		ctx.put("baseUrl", "http://localhost");
		ctx.put("form", new JsonObject(form.removeInactiveElements().toJson()));
		ctx.put("data", form.getReportableFormData(formDataNode, FormElement::isActive, language));
		ctx.put("dataUuid", formDataNode.getUuid());

		String content = renderService.rxRender(ctx, TemplateWrapper.wrap(null, templateName)).blockingGet().toString();
		List<MailRow> rows = getRows(content);
		String description = String.format("Rendered rows in %s (language %s)", templateName, language);

		assertThat(rows).as(description).extracting(MailRow::label).containsExactly(
			"Schema only label",
			de ? "UI-Label der Eingabe" : "UI label of input",
			de ? "Beide Labels: UI-Label" : "Both labels: UI label",
			"Empty UI label: schema label",
			"Teilweise leeres UI-Label",
			de ? "UI-Label der Einzelwahl" : "UI label of single select",
			de ? "UI-Label der Mehrfachwahl" : "UI label of multi select");

		for (int i = 0; i < 5; i++) {
			assertThat(rows.get(i).value()).as("%s: value of row %d", description, i).endsWith("value " + (i + 1));
			assertThat(rows.get(i).listItems()).as("%s: list items of row %d", description, i).isEmpty();
		}

		assertThat(rows.get(5).value()).as("%s: value of single select", description)
			.isEqualTo(de ? "Einzelwahl 2" : "Single option 2");
		assertThat(rows.get(5).listItems()).as("%s: list items of single select", description).isEmpty();

		assertThat(rows.get(6).listItems()).as("%s: list items of multi select", description)
			.containsExactly(de ? "Mehrfachwahl 1" : "Multi option 1", de ? "Mehrfachwahl 3" : "Multi option 3");
	}

	/**
	 * Rendered data row of a mail
	 * @param label text of the label cell
	 * @param value text of the value cell
	 * @param listItems texts of the list items in the value cell
	 */
	private record MailRow(String label, String value, List<String> listItems) {
	}

	/**
	 * Parse the rendered mail and get all data rows (rows with a label and a value cell).
	 *
	 * @param content The rendered mail.
	 * @return list of data rows
	 */
	private List<MailRow> getRows(String content) {
		return Jsoup.parse(content).select("tr").stream()
			.map(Element::children)
			.filter(cells -> cells.size() == 2)
			.map(cells -> new MailRow(cells.get(0).text(), cells.get(1).text(), cells.get(1).select("li").eachText()))
			.collect(Collectors.toList());
	}

	/**
	 * Assert that the row of the element with the given label contains the expected value and is not rendered as list.
	 *
	 * @param templateName The name of the mail template.
	 * @param content The rendered mail.
	 * @param label The label of the element.
	 * @param expectedValue The expected rendered value (or value label) of the element.
	 */
	private void assertSingleValueRow(String templateName, String content, String label, String expectedValue) {
		assertThat(getRow(content, label))
			.as("Rendered row of \"%s\" in %s", label, templateName)
			.contains(expectedValue)
			.doesNotContain("<li>")
			.doesNotContain("false");
	}

	/**
	 * Get the part of the rendered mail between the given element label and the end of its table row.
	 *
	 * @param content The rendered mail.
	 * @param label The label of the element.
	 * @return The rendered row content after the label.
	 */
	private String getRow(String content, String label) {
		assertThat(content)
			.as("Rendered mail")
			.contains(label);

		return StringUtils.substringBefore(StringUtils.substringAfter(content, label), "</tr>");
	}
}
