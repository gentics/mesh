package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gentics.mesh.plugin.forms.model.FormType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.plugin.forms.service.SchemaService;
import com.jayway.jsonpath.JsonPath;

import io.vertx.core.json.JsonObject;
import okhttp3.Request;
import okhttp3.Response;

/**
 * CRUD Tests for forms
 */
@RunWith(value = Parameterized.class)
public class FormCRUDTest extends AbstractFormsPluginIntegrationTest {
	@Parameters(name = "{index}: permission {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Boolean permission : Arrays.asList(true, false)) {
			data.add(new Object[] { permission });
		}
		return data;
	}

	@Parameter(0)
	public boolean permission;

	@Before
	public void setup() {
		super.setup();
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
	}

	/**
	 * Test creating a new form
	 * @throws IOException
	 */
	@Test
	public void testCreateForm() throws IOException {
		String inputFilename = "create.json";
		JsonObject expectedJson = JsonUtil.readValue(fromFile(inputFilename), JsonObject.class);

		assertCreatedForm(inputFilename, expectedJson);
	}

	@Test
	public void testCreateFormNoType() throws IOException {
		String inputFilename = "create_no_type.json";
		JsonObject expectedJson = JsonUtil.readValue(fromFile(inputFilename), JsonObject.class);

		assertThat(expectedJson.getMap().keySet())
			.as("Form fields")
			.doesNotContain("type");

		expectedJson.put("type", FormType.GENERIC.name());

		assertCreatedForm(inputFilename, expectedJson);
	}

	/**
	 * Test creating a new form in german
	 * @throws IOException
	 */
	@Test
	public void testCreateGermanForm() throws IOException {
		String uuid = createForm("create_de.json", permission, true);

		if (permission) {
			NodeResponse nodeResponse = server.client().findNodeByUuid(PROJECT_NAME, uuid, new NodeParametersImpl().setLanguages("de")).blockingGet();

			assertThat(nodeResponse.getProject().getName()).as("NodeResponse Project").isEqualTo(PROJECT_NAME);
			assertThat(nodeResponse.getFields().getStringField("name").getString()).as("NodeResponse name").isEqualTo("Erstelltes Formular");
			JsonObject loadedJson = JsonUtil.readValue(nodeResponse.getFields().getStringField("json").getString(), JsonObject.class);
			JsonObject expectedJson = JsonUtil.readValue(fromFile("create_de.json"), JsonObject.class);
			assertThat(loadedJson).as("NodeResponse json").isEqualTo(expectedJson);

			Optional<NodeResponse> optionalNode = loadNode(nodeResponse.getUuid());
			assertThat(optionalNode).as("Form Node").isPresent();
			assertThat(optionalNode.get().getParentNode().getSchema().getName()).as("Parent schema").isEqualTo(SchemaService.PLUGIN_SCHEMA_NAME);
			assertThat(loadSchema(SchemaService.getSchemaName(nodeResponse.getUuid()))).as("Form Schema").isPresent();
		}
	}


	/**
	 * Test listing forms
	 * @throws IOException
	 */
	@Test
	public void testListForms() throws IOException {
		createForm("list1.json");
		createForm("list2.json");
		createForm("list3.json");

		Request listRequest = new Request.Builder()
				.url(projectUrl("/forms"))
				.headers(auth(permission))
				.get()
				.build();
		Response response = client().newCall(listRequest).execute();

		if (permission) {
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

			String responseBody = response.body().string();
			int totalCount = JsonPath.read(responseBody, "$.totalCount");
			assertThat(totalCount).as("Total count").isEqualTo(3);
			List<String> names = JsonPath.read(responseBody, "$.forms[*].name");
			assertThat(names).as("Form names").containsOnly("Form List 1", "Form List 2", "Form List 3");
		} else {
			assertForbidden(response);
		}
	}

	/**
	 * Test loading a specific form
	 * @throws IOException
	 */
	@Test
	public void testLoadForm() throws IOException {
		String uuid = createForm("create.json");

		JsonNode formJson = doRequest(new Request.Builder()
				.url(projectUrl("/forms/" + uuid))
				.headers(auth(permission))
				.get()
				.build(), permission, true, JsonNode.class);

		if (permission) {
			assertThat(formJson).as("Loaded form").isNotNull();
			assertThat(formJson.get("name")).as("Form name").isNotNull();
			assertThat(formJson.get("name").asText()).as("Form name").isEqualTo("Created Form");
			assertThat(formJson.get("language")).isNull();
		}
	}

	/**
	 * Test loading a specific form in german
	 * @throws IOException
	 */
	@Test
	public void testLoadGermanForm() throws IOException {
		String uuid = createForm("create_de.json");

		JsonNode formJson = doRequest(new Request.Builder()
				.url(projectUrl("/forms/" + uuid) + "?lang=de")
				.headers(auth(permission))
				.get()
				.build(), permission, true, JsonNode.class);

		if (permission) {
			assertThat(formJson).as("Loaded form").isNotNull();
			assertThat(formJson.get("name")).as("Form name").isNotNull();
			assertThat(formJson.get("name").asText()).as("Form name").isEqualTo("Erstelltes Formular");
			assertThat(formJson.get("language")).as("Form language").isNotNull();
			assertThat(formJson.get("language").asText()).as("Form language").isEqualTo("de");
		}
	}

	/**
	 * Test updating a form
	 * @throws IOException
	 */
	@Test
	public void testUpdateForm() throws IOException {
		NodeResponse updatedNode = updateForm("update.json", permission, true);

		if (permission) {
			assertThat(updatedNode.getDisplayName())
				.as("Name of updated form")
				.isEqualTo("Updated Form");
		}
	}

	/**
	 * Test deleting a form
	 * @throws IOException
	 */
	@Test
	public void testDeleteForm() throws IOException {
		String uuid = createForm("create.json");

		Request deleteRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid))
				.headers(auth(permission))
				.delete()
				.build();
		Response response = client().newCall(deleteRequest).execute();

		if (permission) {
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

			assertThat(loadNode(uuid)).as("Form Node").isEmpty();
			assertThat(loadSchema(SchemaService.getSchemaName(uuid))).as("Form Schema").isEmpty();
		} else {
			assertForbidden(response);
		}
	}

	private void assertCreatedForm(String inputFilename, JsonObject expectedJson) throws IOException {
		String uuid = createForm(inputFilename, permission, true);

		if (permission) {
			NodeResponse nodeResponse = server.client().findNodeByUuid(PROJECT_NAME, uuid).blockingGet();

			assertThat(nodeResponse.getProject().getName()).as("NodeResponse Project").isEqualTo(PROJECT_NAME);
			assertThat(nodeResponse.getFields().getStringField("name").getString()).as("NodeResponse name").isEqualTo(expectedJson.getString("name"));
			JsonObject loadedJson = JsonUtil.readValue(nodeResponse.getFields().getStringField("json").getString(), JsonObject.class);
			assertThat(loadedJson).as("NodeResponse json").isEqualTo(expectedJson);

			Optional<NodeResponse> optionalNode = loadNode(nodeResponse.getUuid());
			assertThat(optionalNode).as("Form Node").isPresent();
			assertThat(optionalNode.get().getParentNode().getSchema().getName()).as("Parent schema").isEqualTo(SchemaService.PLUGIN_SCHEMA_NAME);
			assertThat(loadSchema(SchemaService.getSchemaName(nodeResponse.getUuid()))).as("Form Schema").isPresent();
		}
	}
}
