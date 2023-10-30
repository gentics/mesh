package com.gentics.mesh.cache;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NameCacheTest extends AbstractMeshTest {

	@Test
	public void testProjectCreation() {
		String projectName = "bogus";
		ProjectResponse project = call(() -> client().findProjectByName(projectName));

		assertNull(project);

		ProjectCreateRequest request = new ProjectCreateRequest().setName(projectName).setSchemaRef("folder");
		project = call(() -> client().createProject(request));

		project = call(() -> client().findProjectByName(projectName));

		assertNotNull(project);
	}
}
