package com.gentics.mesh.core.verticle;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.test.AbstractRestEndpointTest;

public class CrossEndpointTest extends AbstractRestEndpointTest {

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
