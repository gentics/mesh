package com.gentics.mesh.core.project;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.verticle.project.ProjectInfoVerticle;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

public class ProjectInfoVerticleTest extends AbstractIsolatedRestVerticleTest {

	private ProjectInfoVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	public void testReadProjectByName() {
		ProjectResponse project = call(() -> getClient().findProjectByName(PROJECT_NAME));
		assertEquals(PROJECT_NAME, project.getName());
	}

}
