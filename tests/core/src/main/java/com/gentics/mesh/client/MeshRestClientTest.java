package com.gentics.mesh.client;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(elasticsearch = TRACKING, testSize = PROJECT_AND_NODE, startServer = true)
public class MeshRestClientTest extends AbstractMeshTest {

	@Test
	public void testGenericGet() {
		JsonObject users = call(() -> client().get("/users"));
		assertNotNull(users.getJsonArray("data"));

		UserListResponse list = call(() -> client().get("/users", UserListResponse.class));
		assertTrue(list.getData().size() != 0);
	}

	@Test
	public void testGenericPost() {
		JsonObject userJson = new JsonObject();
		userJson.put("username", "joedoe123");
		userJson.put("password", "finger123");
		JsonObject json2 = call(() -> client().post("/users", userJson));
		assertEquals("joedoe123", json2.getString("username"));

		UserCreateRequest userCreateRequest = new UserCreateRequest();
		userCreateRequest.setUsername("joedoe2");
		userCreateRequest.setPassword("finger1234");
		UserResponse response = call(() -> client().post("/users", userCreateRequest, UserResponse.class));
		assertEquals("joedoe2", response.getUsername());

		// TODO stub server and test other methods
		// JsonObject json = call(() -> client().post("/test"));
		// UserResponse response = call(() -> client().post("/users", UserResponse.class));
	}

	@Test
	@Ignore
	public void testGenericPut() {
		// TODO Stub server and use put - we currently have no routes which accept put
		JsonObject json = call(() -> client().put("/test"));
		UserResponse response = call(() -> client().put("/users", UserResponse.class));
		JsonObject userJson = new JsonObject();
		JsonObject json2 = call(() -> client().put("/users", userJson));

		UserCreateRequest userCreateRequest = new UserCreateRequest();
		userCreateRequest.setUsername("joedoe2");
		call(() -> client().put("/users", userCreateRequest, UserResponse.class));
	}

	@Test
	public void testGenericDelete() {
		UserCreateRequest userCreateRequest = new UserCreateRequest();
		userCreateRequest.setUsername("joedoe2");
		userCreateRequest.setPassword("finger1234");
		//String userUuid1 = call(() -> client().createUser(userCreateRequest)).getUuid();
		// TODO stub server and return content on delete - we currently return 204
		// call(() -> client().delete("/users/" + userUuid1));

		String userUuid2 = call(() -> client().createUser(userCreateRequest)).getUuid();
		call(() -> client().deleteEmpty("/users/" + userUuid2));

		//String userUuid3 = call(() -> client().createUser(userCreateRequest)).getUuid();
		//UserResponse response = call(() -> client().delete("/users/" + userUuid3, UserResponse.class));

	}

}
