package com.gentics.mesh.core.webroot;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.call;

@MeshTestSetting(testSize = FULL, startServer = true)
public class CrossEndpointTest extends AbstractMeshTest {

	@Test
	public void testAccessNewProjectRoute() {
		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setSchema(new SchemaReferenceImpl().setName("folder"));
		request.setName(name);

		call(() -> client().createProject(request));
		call(() -> client().findNodes(name));
		call(() -> client().findTagFamilies(name));
	}

}
