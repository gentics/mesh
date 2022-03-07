package com.gentics.mesh.core.project;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.util.URIUtils.encodeSegment;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = PROJECT, startServer = true)
public class ProjectInfoEndpointTest extends AbstractMeshTest {

	@Test
	public void testReadProjectByName() {
		ProjectResponse project = call(() -> client().findProjectByName(PROJECT_NAME));
		assertEquals(PROJECT_NAME, project.getName());
	}

	/**
	 * Test reading an unknown project by name
	 */
	@Test
	public void testReadUnknownProjectByName() {
		call(() -> client().findProjectByName("Unknown"), NOT_FOUND, "object_not_found_for_name", "Unknown");
	}

	/**
	 * Test trying to read a project by name with an end slash
	 */
	@Test
	public void testReadProjectWithEndSlash() {
		call(() -> client().get("/" + encodeSegment(PROJECT_NAME) + "/", ProjectResponse.class), NOT_FOUND);
	}
}
