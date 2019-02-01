package com.gentics.mesh.search.raw.project;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertNotNull;

import io.vertx.core.json.JsonObject;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = true, startServer = true, testSize = FULL)
public class ProjectTagRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() throws JSONException {
		String tagName = "newtag";

		// Create two tag with the same name each in a different project
		String tagFamilyName = "newtagfamily";
		ProjectResponse projectA = createProject("projectA");
		TagFamilyResponse tagFamilyA = createTagFamily(projectA.getName(), tagFamilyName);
		TagResponse tagA = createTag("projectA", tagFamilyA.getUuid(), tagName);

		ProjectResponse projectB = createProject("projectB");
		TagFamilyResponse tagFamilyB = createTagFamily(projectB.getName(), tagFamilyName);
		createTag("projectB", tagFamilyB.getUuid(), tagName);

		String query = getSimpleTermQuery("name.raw", tagName);

		JsonObject response = new JsonObject(call(() -> client().searchTagsRaw("projectA", query)).toString());
		assertNotNull(response);
		assertThat(response).has("responses[0].hits.hits[0]._id", tagA.getUuid(), "The correct element was not found.");
		assertThat(response).has("responses[0].hits.total", "1", "Not exactly one item was found");

	}
}
