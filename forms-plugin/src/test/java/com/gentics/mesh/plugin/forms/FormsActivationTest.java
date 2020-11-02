package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.plugin.forms.service.SchemaService;

/**
 * Test cases for activation of forms plugin
 */
public class FormsActivationTest extends AbstractFormsPluginIntegrationTest {
	@Test
	public void testActivate() {
		// assert that plugin is not activated
		assertThat(getStatus(PROJECT_NAME)).as("Activation status").isFalse();

		// forms schemas must not exist
		SchemaListResponse schemas = call(() -> server.client().findSchemas(new PagingParametersImpl().setPerPage(Long.MAX_VALUE)));
		assertThat(schemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("List of schemas")
				.doesNotContain(SchemaService.PLUGIN_SCHEMA_NAME, SchemaService.SCHEMA_NAME);

		// activate for project
		assertThat(activate(PROJECT_NAME)).as("Status after activating").isTrue();

		// forms schemas must exist now
		schemas = call(() -> server.client().findSchemas(new PagingParametersImpl().setPerPage(Long.MAX_VALUE)));
		assertThat(schemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("List of schemas")
				.contains(SchemaService.PLUGIN_SCHEMA_NAME, SchemaService.SCHEMA_NAME);

		// assert schemas for project
		schemas = call(() -> server.client().findSchemas(PROJECT_NAME, new PagingParametersImpl().setPerPage(Long.MAX_VALUE)));
		assertThat(schemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("List of schemas for project")
				.contains(SchemaService.PLUGIN_SCHEMA_NAME, SchemaService.SCHEMA_NAME);
	}

	@Test
	public void testDeactivate() {
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();

		// deactivate for project
		assertThat(deActivate(PROJECT_NAME)).as("Status after deactivating").isFalse();

		// forms schemas must still exist
		SchemaListResponse schemas = call(() -> server.client().findSchemas(new PagingParametersImpl().setPerPage(Long.MAX_VALUE)));
		assertThat(schemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("List of schemas")
				.contains(SchemaService.PLUGIN_SCHEMA_NAME, SchemaService.SCHEMA_NAME);

		// schemas must still be assigned to project
		schemas = call(() -> server.client().findSchemas(PROJECT_NAME, new PagingParametersImpl().setPerPage(Long.MAX_VALUE)));
		assertThat(schemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("List of schemas for project")
				.contains(SchemaService.PLUGIN_SCHEMA_NAME, SchemaService.SCHEMA_NAME);
	}

	@Test
	public void testSetLanguagesActive() {
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();

		assertThat(getLanguages(PROJECT_NAME)).as("Initial languages").isNull();

		List<String[]> sets = Arrays.asList(new String[] { "de", "en", "fr" }, new String[] { "en", "fr", "de" },
				new String[] { "de", "en" }, new String[] { "hr", "en", "de" });

		for (String[] languages : sets) {
			// set
			assertThat(setLanguages(PROJECT_NAME, Arrays.asList(languages))).containsExactly(languages);
			// get
			assertThat(getLanguages(PROJECT_NAME)).containsExactly(languages);
		}
	}
}
