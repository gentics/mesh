package com.gentics.mesh.auth;

import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@MeshTestSetting(testSize = PROJECT_AND_NODE, startServer = true, useKeycloak = true)
public class OAuthKeycloakTest extends AbstractMeshTest {

	@ClassRule
	public static TestRule rule = (Statement base, Description description) -> {
		testContext.getOptions().getAuthenticationOptions().getOauth2().setMapperScriptPath("src/test/resources/oauth2/mapperscript.js");
		return base;
	};

	@Test
	public void testKeycloakAuth() throws Exception {

		// 1. Login the user
		JsonObject authInfo = loginKeycloak();
		System.out.println("Login Token\n:" + authInfo.encodePrettily());

		// 2. Invoke authenticated request
		JsonObject info = get("/api/v1/auth/me", authInfo);

		UserResponse me = JsonUtil.readValue(info.encodePrettily(), UserResponse.class);

		System.out.println(info.encodePrettily());
		assertEquals("dummy@dummy.dummy", me.getEmailAddress());
		assertEquals("Dummy", me.getFirstname());
		assertEquals("User", me.getLastname());
		assertEquals("dummyuser", me.getUsername());
		String uuid = me.getUuid();

		// 3. Invoke request again to ensure that the previously created user gets returned
		JsonObject info2 = get("/api/v1/auth/me", authInfo);
		UserResponse me2 = JsonUtil.readValue(info2.encodePrettily(), UserResponse.class);
		assertEquals("The uuid should not change. The previously created user should be returned.", uuid, me2.getUuid());

		assertEquals("group1", me2.getGroups().get(0).getName());
		assertEquals("group2", me2.getGroups().get(1).getName());

		assertNotNull(tx(() -> boot().groupRoot().findByName("group1")));
		assertNotNull(tx(() -> boot().groupRoot().findByName("group2")));

		assertNotNull(tx(() -> boot().roleRoot().findByName("role1")));
		assertNotNull(tx(() -> boot().roleRoot().findByName("role2")));

		System.out.println(get("/api/v1/auth/me"));

		// client().logout();
		// UserResponse me = call(() -> client().me());
		// System.out.println(me.toJson());
	}

	protected JsonObject get(String path, JsonObject authInfo) throws IOException {
		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.header("Authorization", "Bearer " + authInfo.getString("access_token"))
			.url("http://localhost:" + testContext.getPort() + path)
			.build();

		Response response = httpClient().newCall(request).execute();
		return new JsonObject(response.body().string());
	}

	protected String get(String path) throws IOException {
		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.url("http://localhost:" + testContext.getPort() + path)
			.build();

		Response response = httpClient().newCall(request).execute();
		System.out.println("Response: " + response.code());
		return response.body().string();
	}

	protected JsonObject loadJson(String path) throws IOException {
		return new JsonObject(IOUtils.toString(getClass().getResource(path), Charset.defaultCharset()));
	}

	private JsonObject loginKeycloak() throws IOException {
		String secret = "9b65c378-5b4c-4e25-b5a1-a53a381b5fb4";

		int port = MeshTestContext.getKeycloak().getFirstMappedPort();

		StringBuilder content = new StringBuilder();
		content.append("client_id=mesh&");
		content.append("username=dummyuser&");
		content.append("password=finger&");
		content.append("grant_type=password&");
		content.append("client_secret=" + secret);
		RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), content.toString());
		// .header("Content-Type", "application/x-www-form-urlencoded")
		Request request = new Request.Builder()
			.post(body)
			.url("http://localhost:" + port + "/auth/realms/master-test/protocol/openid-connect/token")
			.build();

		Response response = httpClient().newCall(request).execute();
		return new JsonObject(response.body().string());
	}
}
