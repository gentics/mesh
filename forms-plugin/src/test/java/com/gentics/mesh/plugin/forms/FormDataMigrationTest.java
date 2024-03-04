package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;

/**
 * Test cases for the migration of form data nodes from direct children of the form node
 * to children of the form data folder
 */
public class FormDataMigrationTest extends AbstractFormsPluginIntegrationTest {
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
		// create a form
		String formUuid = createForm("inputfield.json");

		// post some data
		submit(formUuid, true, true, "input1", "");
		submit(formUuid, "input1", "one");
		submit(formUuid, "input1", "two");
		submit(formUuid, "input1", "three");

		// undeploy forms plugin
		server.getMesh().<MeshComponent>internal().pluginManager().undeploy("forms").blockingAwait();

		// move data nodes
		List<NodeResponse> dataNodes = getFormDataNodes(formUuid);
		assertThat(dataNodes).as("Form data nodes").isNotEmpty();
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
