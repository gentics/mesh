package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.plugin.forms.service.SchemaService;

import io.reactivex.Observable;

/**
 * Test cases for activation of forms plugin
 */
@RunWith(Parameterized.class)
public class FormsStatusTest extends AbstractFormsPluginIntegrationTest {
	@Parameters(name = "{index}: pathPrefix {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (String pathPrefix : Arrays.asList("", "/path/prefix")) {
			data.add(new Object[] { pathPrefix });
		}
		return data;
	}

	@Parameter(0)
	public String pathPrefix;

	@Before
	public void setup() {
		super.setup();
		server.client().findBranches(PROJECT_NAME).toSingle()
				.flatMapObservable(branchesListResponse -> Observable.fromIterable(branchesListResponse.getData()))
				.filter(branch -> branch.getLatest() != null && branch.getLatest()).firstOrError()
				.flatMap(branch -> server.client().updateBranch(PROJECT_NAME, branch.getUuid(),
						new BranchUpdateRequest().setPathPrefix(pathPrefix)).toSingle()).blockingGet();
	}

	@Test
	public void testActivate() {
		// assert that plugin is not activated
		assertThat(getStatus(PROJECT_NAME).isActive()).as("Activation status").isFalse();

		// forms schemas must not exist
		SchemaListResponse schemas = call(() -> server.client().findSchemas(new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		assertThat(schemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("List of schemas")
				.doesNotContain(SchemaInitializer.PLUGIN_SCHEMA_NAME, SchemaInitializer.DOWNLOAD_SCHEMA_NAME, SchemaInitializer.FOLDER_SCHEMA_NAME, SchemaInitializer.SCHEMA_NAME);

		// activate for project
		assertThat(activate(PROJECT_NAME)).as("Status after activating").isTrue();

		// forms schemas must exist now
		schemas = call(() -> server.client().findSchemas(new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		assertThat(schemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("List of schemas")
				.contains(SchemaInitializer.PLUGIN_SCHEMA_NAME, SchemaInitializer.DOWNLOAD_SCHEMA_NAME, SchemaInitializer.FOLDER_SCHEMA_NAME, SchemaInitializer.SCHEMA_NAME);

		// assert schemas for project
		schemas = call(() -> server.client().findSchemas(PROJECT_NAME, new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		assertThat(schemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("List of schemas for project")
				.contains(SchemaInitializer.PLUGIN_SCHEMA_NAME, SchemaInitializer.DOWNLOAD_SCHEMA_NAME, SchemaInitializer.FOLDER_SCHEMA_NAME, SchemaInitializer.SCHEMA_NAME);
	}

	@Test
	public void testDeactivate() {
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();

		// deactivate for project
		assertThat(deActivate(PROJECT_NAME)).as("Status after deactivating").isFalse();

		// forms schemas must still exist
		SchemaListResponse schemas = call(() -> server.client().findSchemas(new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		assertThat(schemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("List of schemas")
				.contains(SchemaInitializer.PLUGIN_SCHEMA_NAME, SchemaInitializer.DOWNLOAD_SCHEMA_NAME, SchemaInitializer.FOLDER_SCHEMA_NAME, SchemaInitializer.SCHEMA_NAME);

		// schemas must still be assigned to project
		schemas = call(() -> server.client().findSchemas(PROJECT_NAME, new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		assertThat(schemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("List of schemas for project")
				.contains(SchemaInitializer.PLUGIN_SCHEMA_NAME, SchemaInitializer.DOWNLOAD_SCHEMA_NAME, SchemaInitializer.FOLDER_SCHEMA_NAME, SchemaInitializer.SCHEMA_NAME);
	}

	@Test
	public void testSetNoIndex() {
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();

		// initially, no flag is set
		assertThat(getStatus(PROJECT_NAME).isNoIndex()).as("Initial noIndex status").isFalse();

		// set the flag
		assertThat(setNoIndex(PROJECT_NAME, true)).as("noIndex status").isTrue();

		// base forms schemas must exist with the flag set
		SchemaListResponse schemas = call(() -> server.client().findSchemas(PROJECT_NAME, new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		assertThat(schemas.getData().stream()
				.filter(schema -> List.of(SchemaInitializer.PLUGIN_SCHEMA_NAME /*, SchemaInitializer.DOWNLOAD_SCHEMA_NAME, SchemaInitializer.FOLDER_SCHEMA_NAME, SchemaInitializer.SCHEMA_NAME*/).contains(schema.getName()))
				.map(SchemaResponse::getNoIndex).reduce(0, (sum, new_) -> {
					return Boolean.TRUE.equals(new_) ? (sum+1) : sum;
				}, (old, new_) -> {
					return old;
				})).as("Global noIndex=true status count for all form schemas").isEqualTo(1);
		
		// remove the flag
		assertThat(setNoIndex(PROJECT_NAME, false)).as("noIndex status").isFalse();

		// base forms schemas must exist with the flag unset
		schemas = call(() -> server.client().findSchemas(PROJECT_NAME, new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		assertThat(schemas.getData().stream()
				.filter(schema -> List.of(SchemaInitializer.PLUGIN_SCHEMA_NAME /*, SchemaInitializer.DOWNLOAD_SCHEMA_NAME, SchemaInitializer.FOLDER_SCHEMA_NAME, SchemaInitializer.SCHEMA_NAME*/).contains(schema.getName()))
				.map(SchemaResponse::getNoIndex).reduce(0, (sum, new_) -> {
					return Boolean.FALSE.equals(new_) ? (sum+1) : sum;
				}, (old, new_) -> {
					return old;
				})).as("Global noIndex=true status count for all form schemas").isEqualTo(1);
	}


	@Test
	public void testSetNoIndexForForm() throws IOException, InterruptedException {
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();

		// initially, no languages are activated
		assertThat(getStatus(PROJECT_NAME).isNoIndex()).as("Initial noIndex status").isFalse();

		String enUuid = createForm("inputfield_languages_en.json");

		// set the flag
		assertThat(setNoIndex(PROJECT_NAME, true)).as("noIndex status").isTrue();

		// forms schemas must exist with the flag set
		SchemaListResponse schemas = call(() -> server.client().findSchemas(PROJECT_NAME, new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		assertThat(schemas.getData().stream()
				.filter(schema -> List.of(SchemaService.getSchemaName(enUuid)).contains(schema.getName()))
				.map(SchemaResponse::getNoIndex).reduce(0, (sum, new_) -> {
					return Boolean.TRUE.equals(new_) ? (sum+1) : sum;
				}, (old, new_) -> {
					return old;
				})).as("Global noIndex=true status count for a form schema").isEqualTo(1);

		String itUuid = createForm("inputfield.json");

		// forms schemas must exist with the flag set
		schemas = call(() -> server.client().findSchemas(PROJECT_NAME, new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		assertThat(schemas.getData().stream()
				.filter(schema -> List.of(SchemaService.getSchemaName(enUuid), SchemaService.getSchemaName(itUuid)).contains(schema.getName()))
				.map(SchemaResponse::getNoIndex).reduce(0, (sum, new_) -> {
					return Boolean.TRUE.equals(new_) ? (sum+1) : sum;
				}, (old, new_) -> {
					return old;
				})).as("Global noIndex=true status count for all form schemas").isEqualTo(2);

		// wait for the migration jobs
		Thread.sleep(2000);

		// remove the flag
		assertThat(setNoIndex(PROJECT_NAME, false)).as("noIndex status").isFalse();

		// forms schemas must exist with the flag unset
		schemas = call(() -> server.client().findSchemas(PROJECT_NAME, new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		assertThat(schemas.getData().stream()
				.filter(schema -> List.of(SchemaService.getSchemaName(enUuid), SchemaService.getSchemaName(itUuid)).contains(schema.getName()))
				.map(SchemaResponse::getNoIndex).reduce(0, (sum, new_) -> {
					return Boolean.FALSE.equals(new_) ? (sum+1) : sum;
				}, (old, new_) -> {
					return old;
				})).as("Global noIndex=false status count for all form schemas").isEqualTo(2);
	}
}
