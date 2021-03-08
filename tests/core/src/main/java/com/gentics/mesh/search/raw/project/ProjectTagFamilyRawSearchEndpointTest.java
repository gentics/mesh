package com.gentics.mesh.search.raw.project;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;
@MeshTestSetting(elasticsearch = CONTAINER_ES6, startServer = true, testSize = FULL)
public class ProjectTagFamilyRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() {

		ProjectResponse project = createProject("projectB");

		// Create two tag families with the same name
		String tagFamilyName = "newtagfamily";
		createTagFamily(project.getName(), tagFamilyName);
		TagFamilyResponse tagFamily2 = createTagFamily(PROJECT_NAME, tagFamilyName);

		waitForSearchIdleEvent();

		String query = getSimpleTermQuery("name.raw", tagFamilyName);
		JsonObject response = new JsonObject(call(() -> client().searchTagFamiliesRaw(PROJECT_NAME, query)).toString());
		assertNotNull(response);
		assertThat(response).has("responses[0].hits.hits[0]._id", tagFamily2.getUuid(), "The correct element was not found.");

		String path = "responses[0].hits.total";
		if (complianceMode() == ComplianceMode.ES_7) {
			path = "responses[0].hits.total.value";
		}
		assertThat(response).has(path, "1", "Not exactly one item was found");
	}
}
