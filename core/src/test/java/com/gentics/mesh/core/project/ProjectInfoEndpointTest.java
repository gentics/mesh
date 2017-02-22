package com.gentics.mesh.core.project;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = true)
public class ProjectInfoEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadProjectByName() {
		ProjectResponse project = call(() -> client().findProjectByName(PROJECT_NAME));
		assertEquals(PROJECT_NAME, project.getName());
	}

}
