package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.plugin.forms.handler.LoadFilesZipHandler;
import com.gentics.mesh.plugin.forms.service.SchemaService;
import com.jayway.jsonpath.JsonPath;

import okhttp3.Request;
import okhttp3.Response;

@RunWith(value = Parameterized.class)
public class GenericFormsPluginIntegrationTest extends AbstractFormsPluginIntegrationTest {
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
	 * Test loading the schema for a form
	 *
	 * TODO: This test keeps failing due to a timing issue. The schema seems not
	 * to be ready yet when the load schema request is sent.
	 *
	 * @throws IOException
	 */
	@Test
	public void testLoadSchema() throws IOException {
		String uuid = createForm("withCmsId.json");

		Request loadSchemaRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid + "/schema"))
				.headers(auth(permission))
				.get()
				.build();
		Response response = client().newCall(loadSchemaRequest).execute();

		if (permission) {
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

			String responseBody = response.body().string();
			String schemaName = JsonPath.read(responseBody, "$.name");
			assertThat(schemaName).as("Schema name").isEqualTo(SchemaService.getSchemaName(uuid));
			int cmsId = JsonPath.read(responseBody, "$.cms_id");
			assertThat(cmsId).as("CMS ID").isEqualTo(4711);
		} else {
			assertForbidden(response);
		}
	}

	/**
	 * Test that the form schema is updated, when the form is changed
	 * @throws IOException
	 */
	@Test
	public void testUpdateFormSchema() throws IOException {
		String uuid = createForm("inputfield.json");

		Optional<SchemaResponse> optionalSchema = loadSchema(SchemaService.getSchemaName(uuid));
		assertThat(optionalSchema).as("Schema").isPresent();
		assertThat(optionalSchema.get().getFields()).as("Schema Fields").usingFieldByFieldElementComparator()
				.containsExactly(new StringFieldSchemaImpl().setName("input1").setLabel("input1"), new StringFieldSchemaImpl().setName("errors").setLabel("Errors"));

		Request updateRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid))
				.headers(auth(permission))
				.post(requestBodyFromFile("threefields.json"))
				.build();
		Response response = client().newCall(updateRequest).execute();

		if (permission) {
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

			optionalSchema = loadSchema(SchemaService.getSchemaName(uuid));
			assertThat(optionalSchema).as("Schema").isPresent();
			assertThat(optionalSchema.get().getFields()).as("Schema Fields").usingFieldByFieldElementComparator()
					.containsExactly(
							new StringFieldSchemaImpl().setName("input1").setLabel("input1"),
							new StringFieldSchemaImpl().setName("input2").setLabel("input2"),
							new StringFieldSchemaImpl().setName("input3").setLabel("input3"),
							new StringFieldSchemaImpl().setName("errors").setLabel("Errors"));
		} else {
			assertForbidden(response);
		}
	}

	@Test
	public void testFilenameUnique() {
		Map<String, String> filenameReplacements = new HashMap<>();
		filenameReplacements.put("list.json", "list (1).json");
		filenameReplacements.put("list (1).json", "list (2).json");
		filenameReplacements.put("list", "list (1)");
		filenameReplacements.put("list (1)", "list (2)");

		for (Map.Entry<String, String> entry : filenameReplacements.entrySet()) {
			String orig = entry.getKey();
			String replacement = LoadFilesZipHandler.getNextFilename(orig);
			String expected = entry.getValue();
			assertThat(replacement).as(String.format("Replacement for %s", orig)).isEqualTo(expected);
		}
	}
}
