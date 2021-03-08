package com.gentics.mesh.search.raw;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;

import org.junit.Test;

import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;
@MeshTestSetting(elasticsearch = CONTAINER_ES6, startServer = true, testSize = FULL)
public class TagRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() {

		String tagName = "newtag";
		TagResponse tag = createTag(PROJECT_NAME, tx(() -> tagFamily("colors").getUuid()), tagName);

		waitForSearchIdleEvent();

		String query = getSimpleTermQuery("name.raw", tagName);
		JsonObject response = new JsonObject(call(() -> client().searchTagsRaw(query)).toString());
		assertThat(response).has("responses[0].hits.hits[0]._id", tag.getUuid(), "The correct element was not found.");

	}
}
