package com.gentics.mesh.search.raw;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class RoleRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() {
		String roleName = "rolename42a";
		RoleResponse role = createRole(roleName, db().tx(() -> group().getUuid()));

		String query = getSimpleTermQuery("name.raw", roleName);

		JsonObject response = new JsonObject(call(() -> client().searchRolesRaw(query)).toString());
		assertThat(response).has("responses[0].hits.hits[0]._id", role.getUuid(), "The correct element was not found.");
	}
}
