package com.gentics.mesh.core.verticle;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.AbstractVerticle;

public class CrossVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.projectVerticle());
		list.add(meshDagger.tagFamilyVerticle());
		list.add(meshDagger.nodeVerticle());
		return list;
	}

	@Test
	public void testAccessNewProjectRoute() {
		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setSchemaReference(new SchemaReference().setName("folder"));
		request.setName(name);

		call(() -> getClient().createProject(request));

		call(() -> getClient().findNodes(name));

		call(() -> getClient().findTagFamilies(name));

	}

}
