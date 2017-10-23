package com.gentics.mesh.search.raw;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertNotNull;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class GroupRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() throws JSONException {
		String groupName = "testgroup42a";
		String uuid = createGroup(groupName).getUuid();

		String query = getSimpleTermQuery("uuid", uuid);

		JsonObject response = call(() -> client().searchGroupsRaw(query));
		assertNotNull(response);
		assertThat(response).has("hits.hits[0]._id", uuid, "The correct element was not found.");
	}
}
