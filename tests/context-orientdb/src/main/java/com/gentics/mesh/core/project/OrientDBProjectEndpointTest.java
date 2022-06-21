package com.gentics.mesh.core.project;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(elasticsearch = TRACKING, testSize = TestSize.FULL, startServer = true)
public class OrientDBProjectEndpointTest extends ProjectEndpointTest {


	/**
	 * Test renaming, deleting and re-creating a project (together with project name cache).
	 */
	@Test
	public void testRenameDeleteCreateProject() {
		// create project named "project"
		ProjectResponse project = createProject("project");

		// get tag families of project (this will put project into cache)
		call(() -> client().findTagFamilies("project"));
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(1);

		// rename project to "newproject"
		project = updateProject(project.getUuid(), "newproject");
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(0);

		// get tag families of newproject (this will put project into cache)
		call(() -> client().findTagFamilies("newproject"));
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(1);

		// delete "newproject"
		deleteProject(project.getUuid());
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(0);

		// create (again)
		project = createProject("project");
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(0);

		// get tag families of project
		call(() -> client().findTagFamilies("project"));
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(1);
	}

}
