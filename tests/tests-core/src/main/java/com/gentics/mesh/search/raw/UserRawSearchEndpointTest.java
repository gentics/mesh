package com.gentics.mesh.search.raw;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class UserRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() throws IOException {
		String username = "testuser42a";
		UserResponse userResponse = createUser(username);

		String json = getESText("userWildcard.es");

		waitForSearchIdleEvent();

		JsonObject response = new JsonObject(call(() -> client().searchUsersRaw(json)).toString());
		assertNotNull(response);
		assertThat(response).has("responses[0].hits.hits[0]._id", userResponse.getUuid(), "The correct element was not found.");
	}

}
