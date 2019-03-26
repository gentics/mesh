package com.gentics.mesh.search.raw;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;
@MeshTestSetting(elasticsearch = CONTAINER, startServer = true, testSize = FULL)

public class ProjectRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() {
		final String projectName = "newproject";
		ProjectResponse project = createProject(projectName);

		String query = getSimpleTermQuery("name.raw", projectName);

		JsonObject response = new JsonObject(call(() -> client().searchProjectsRaw(query)).toString());
		assertNotNull(response);
		assertThat(response).has("responses[0].hits.hits[0]._id", project.getUuid(), "The correct element was not found.");
	}
}
