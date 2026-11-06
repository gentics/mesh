package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.plugin.forms.model.response.StatusResponse;
import com.gentics.mesh.plugin.test.common.AbstractPluginLocalIntegrationTest;
import com.gentics.mesh.plugin.test.common.HibernateMeshLocalServer;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import com.gentics.mesh.test.local.MeshLocalServer;

/**
 * Test case for enabling the plugin for a project, that has a non-empty pathPrefix
 */
public class ProjectWithPathPrefixTest extends AbstractPluginLocalIntegrationTest {

	@ClassRule
	public static final MeshLocalServer server = new HibernateMeshLocalServer()
		.waitForStartup();

	@Test
	public void startPlugin() throws Exception {
		server.getMesh().deployPlugin(FormsTestPlugin.class, "forms").blockingAwait();
		PluginTestUtil.waitForPluginRegistration(server, 5_000, "forms");

		// This project is configured
		createProject("testproject");

		// activate the plugin
		server.client().put("/testproject/plugins/forms/active", StatusResponse.class).blockingGet();

		// the plugin's root node is expected to be generated in the project
		assertThat(TestUtil.findRootNode(server.client(), "testproject").blockingGet()).isNotNull();
	}

	/**
	 * Create a project with non-empty pathPrefix
	 * @param projectName name
	 */
	private void createProject(String projectName) {
		server.client().createProject(new ProjectCreateRequest()
			.setName(projectName)
			.setPathPrefix("/notEmpty")
			.setSchemaRef("folder")
		).blockingAwait();
	}
}
