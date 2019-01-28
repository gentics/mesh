package com.gentics.mesh.search.raw;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertNotNull;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class GroupRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() {
		String groupName = "testgroup42a";
		String uuid = createGroup(groupName).getUuid();

		String query = getSimpleTermQuery("uuid", uuid);

		JSONObject response = call(() -> client().searchGroupsRaw(query));
		assertNotNull(response);
		assertThat(response).has("responses[0].hits.hits[0]._id", uuid, "The correct element was not found.");
	}
}
