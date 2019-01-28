package com.gentics.mesh.search.raw;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = true, startServer = true, testSize = FULL)
public class TagRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() throws JSONException {

		String tagName = "newtag";
		TagResponse tag = createTag(PROJECT_NAME, tx(() -> tagFamily("colors").getUuid()), tagName);

		String query = getSimpleTermQuery("name.raw", tagName);
		JSONObject response = call(() -> client().searchTagsRaw(query));
		assertThat(response).has("responses[0].hits.hits[0]._id", tag.getUuid(), "The correct element was not found.");

	}
}
