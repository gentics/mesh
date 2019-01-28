package com.gentics.mesh.search.raw;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;

import java.io.IOException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class RoleRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() throws IOException, JSONException {
		String roleName = "rolename42a";
		RoleResponse role = createRole(roleName, db().tx(() -> group().getUuid()));

		String query = getSimpleTermQuery("name.raw", roleName);

		JSONObject response = call(() -> client().searchRolesRaw(query));
		assertThat(response).has("responses[0].hits.hits[0]._id", role.getUuid(), "The correct element was not found.");
	}
}
