package com.gentics.mesh.core.project;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.test.AbstractRestEndpointTest;

public class ProjectInfoEndpointTest extends AbstractRestEndpointTest {

	@Test
	public void testReadProjectByName() {
		ProjectResponse project = call(() -> getClient().findProjectByName(PROJECT_NAME));
		assertEquals(PROJECT_NAME, project.getName());
	}

}
