package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.plugin.test.common.OrientDBMeshLocalServer;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import com.gentics.mesh.test.local.MeshLocalServer;

/**
 * Tests if the plugin is able to start without a project.
 *
 * https://jira.gentics.com/browse/SUP-9022
 */
public class NoProjectTest {
	@ClassRule
	public static final MeshLocalServer server = new OrientDBMeshLocalServer()
		.withInMemoryMode()
		.waitForStartup();

	@Test
	public void startPluginWithoutProject() throws Exception {
		server.getMesh().deployPlugin(FormsTestPlugin.class, "forms").blockingAwait();
		PluginTestUtil.waitForPluginRegistration(server, 5_000, "forms");

		// This project is configured
		createProject("testproject");
		// This project is NOT configured
		createProject("anotherproject");

		// Wait until plugin has initialized the project
		Thread.sleep(1000);

		TestUtil.findRootNode(server.client(), "testproject").blockingGet();
		boolean absent = TestUtil.findRootNode(server.client(), "anotherproject").isEmpty().blockingGet();
		assertThat(absent).isTrue();
	}

	private void createProject(String projectName) {
		server.client().createProject(new ProjectCreateRequest()
			.setName(projectName)
			.setSchemaRef("folder")
		).blockingAwait();
	}


}
