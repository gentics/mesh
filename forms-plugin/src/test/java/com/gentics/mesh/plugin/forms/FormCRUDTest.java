package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.mesh.util.UUIDUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.mesh.plugin.forms.model.FormType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
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
		setLanguages(PROJECT_NAME, Arrays.asList("en", "de", "it"));

		createForm("list1.json");
		createForm("list2.json");
		createForm("list3.json");

		doTestListForms(new String[0], new String[] { "Form List 1", "Form List 2", "Form List 3" });
		doTestListForms(new String[] { "en" }, new String[] { "Form List 1" });
		doTestListForms(new String[] { "de" }, new String[] { "Form List 1", "Form List 2" });
		doTestListForms(new String[] { "it" }, new String[] { "Form List 1", "Form List 3" });
		doTestListForms(new String[] { "en", "de" }, new String[] { "Form List 1", "Form List 2" });
		doTestListForms(new String[] { "en", "it" }, new String[] { "Form List 1", "Form List 3" });
		doTestListForms(new String[] { "de", "it" }, new String[] { "Form List 1", "Form List 2", "Form List 3" });
		doTestListForms(new String[] { "en", "de", "it" }, new String[] { "Form List 1", "Form List 2", "Form List 3" });
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
	 * Test that updating a form with missing schema will create the schema
	 * @throws IOException
	 */
	@Test
	public void testUpdateFormWithMissingSchema() throws IOException {
		NodeResponse updatedNode = updateForm("update.json", permission, true, uuid -> {
			Optional<SchemaResponse> optSchema = loadGlobalSchema("form_%s".formatted(uuid));
			assertThat(optSchema).as("Schema").isPresent();
			call(() -> server.client().deleteSchema(optSchema.get().getUuid()));
		});

		if (permission) {
			assertThat(updatedNode.getDisplayName())
				.as("Name of updated form")
				.isEqualTo("Updated Form");
			Optional<SchemaResponse> optSchema = loadSchema("form_%s".formatted(updatedNode.getUuid()));
			assertThat(optSchema).as("Schema").isPresent();
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

	/**
	 * Test deleting a form with data
	 * @throws IOException
	 */
	@Test
	public void testDeleteFormWithData() throws IOException {
		String uuid = createForm("inputfield.json");

		for (int i = 0; i < 20; i++) {
			boolean empty = i % 2 == 0;
			submit(uuid, true, empty, "input1", empty ? "" : "data %d".formatted(i));
		}

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

	/**
	 * Test deleting a form
	 * @throws IOException
	 */
	@Test
	public void givenDeleteNotExistingForm_shouldNotReturn404() throws IOException {
		String uuid = UUIDUtil.randomUUID();

		Request deleteRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid))
				.headers(auth(permission))
				.delete()
				.build();
		Response response = client().newCall(deleteRequest).execute();

		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(loadNode(uuid)).as("Form Node").isEmpty();
		assertThat(loadSchema(SchemaService.getSchemaName(uuid))).as("Form Schema").isEmpty();
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

	/**
	 * Perform a list forms request for the given language.
	 *
	 * @param lang The language to add to the request.
	 * @param expectedForms The forms expected to be returned.
	 */
	private void doTestListForms(String[] lang, String[] expectedForms) throws IOException {
		String langParams = Arrays.stream(lang).map(l -> String.format("lang=%s", l)).collect(Collectors.joining("&", "?", ""));
		Request listRequest = new Request.Builder()
			.url(projectUrl("/forms") + langParams)
			.headers(auth(permission))
			.get()
			.build();
		Response response = client().newCall(listRequest).execute();

		if (permission) {
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

			String responseBody = response.body().string();
			int totalCount = JsonPath.read(responseBody, "$.totalCount");
			assertThat(totalCount).as("Total count").isEqualTo(expectedForms.length);
			List<String> names = JsonPath.read(responseBody, "$.forms[*].name");
			assertThat(names).as("Form names").containsOnly(expectedForms);
		} else {
			assertForbidden(response);
		}
	}
}
