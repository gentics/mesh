package com.gentics.mesh.core.project;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.AbstractVerticle;

public class ProjectInfoVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.projectInfoVerticle());
		return list;
	}

	@Test
	public void testReadProjectByName() {
		ProjectResponse project = call(() -> getClient().findProjectByName(PROJECT_NAME));
		assertEquals(PROJECT_NAME, project.getName());
	}

}
