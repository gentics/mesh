package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
