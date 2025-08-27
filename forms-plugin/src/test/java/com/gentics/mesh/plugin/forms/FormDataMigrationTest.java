package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;

/**
 * Test cases for the migration of form data nodes from direct children of the form node
 * to children of the form data folder
 */
public class FormDataMigrationTest extends AbstractFormsPluginIntegrationTest {
	/**
	 * Number of data nodes to migrate
	 */
	protected final static int NUM_DATA = 105;

	/**
	 * Page size for migration
	 */
	protected final static int PER_PAGE = 10;

	@Before
	public void setup() {
		super.setup();

		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
		setLanguages(PROJECT_NAME, Arrays.asList("en"));
	}

	/**
	 * Test that all misplaced form data nodes will be moved to the form data folder during plugin deployment
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testMigration() throws IOException, InterruptedException {
		// set the page size for the migration
		System.setProperty(FormDataService.MESH_FORMS_PLUGIN_MAX_CONCURRENT_MIGRATION_FETCH_NODES, Integer.toString(PER_PAGE));

		// create a form
		String formUuid = createForm("inputfield.json");

		// post some data
		for (int i = 0; i < NUM_DATA; i++) {
			boolean empty = RandomUtils.nextInt(0, 2) == 0;
			submit(formUuid, true, empty, "input1", empty ? "" : "data %d".formatted(i));
		}

		// undeploy forms plugin
		server.getMesh().<MeshComponent>internal().pluginManager().undeploy("forms").blockingAwait();

		// move data nodes
		List<NodeResponse> dataNodes = getFormDataNodes(formUuid);
		assertThat(dataNodes).as("Form data nodes").isNotEmpty().hasSize(NUM_DATA);
		Set<String> formDataNodeUuids = dataNodes.stream().map(NodeResponse::getUuid).collect(Collectors.toSet());
		dataNodes.stream().forEach(dataNode -> {
			call(() -> server.client().moveNode(PROJECT_NAME, dataNode.getUuid(), formUuid));
		});
		// assert that all nodes have been moved away
		dataNodes = getFormDataNodes(formUuid);
		assertThat(dataNodes).as("Form data nodes").isEmpty();

		// deploy forms plugin
		server.getMesh().deployPlugin(FormsTestPlugin.class, "forms").blockingAwait();
		PluginTestUtil.waitForPluginRegistration(server, 5_000, "forms");

		// assert that data nodes have been moved to correct place during deployment
		dataNodes = getFormDataNodes(formUuid);
		assertThat(dataNodes).as("Form data nodes").isNotEmpty();
		Set<String> migratedDataNodeUuids = dataNodes.stream().map(NodeResponse::getUuid).collect(Collectors.toSet());
		assertThat(migratedDataNodeUuids).as("UUIDs of migrated form data nodes").containsOnlyElementsOf(formDataNodeUuids);
	}
}
