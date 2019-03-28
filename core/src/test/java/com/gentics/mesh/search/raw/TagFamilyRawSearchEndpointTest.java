package com.gentics.mesh.search.raw;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;
@MeshTestSetting(elasticsearch = CONTAINER, startServer = true, testSize = FULL)
public class TagFamilyRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() {

		String tagFamilyName = "newtagfamily";
		TagFamilyResponse tagFamily = createTagFamily(PROJECT_NAME, tagFamilyName);

		String query = getSimpleTermQuery("name.raw", tagFamilyName);

		waitForSearchIdleEvent();

		JsonObject response = new JsonObject(call(() -> client().searchTagFamiliesRaw(query)).toString());
		assertNotNull(response);
		assertThat(response).has("responses[0].hits.hits[0]._id", tagFamily.getUuid(), "The correct element was not found.");
	}
}
