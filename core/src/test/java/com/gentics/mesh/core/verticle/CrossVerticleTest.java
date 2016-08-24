package com.gentics.mesh.core.verticle;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.AbstractVerticle;

public class CrossVerticleTest extends AbstractIsolatedRestVerticleTest {

	private ProjectVerticle projectVerticle;

	private TagFamilyVerticle tagFamilyVerticle;

	private NodeVerticle nodeVerticle;

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(projectVerticle);
		list.add(tagFamilyVerticle);
		list.add(nodeVerticle);
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
