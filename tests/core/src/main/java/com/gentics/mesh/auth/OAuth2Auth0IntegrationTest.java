package com.gentics.mesh.auth;

import static com.gentics.mesh.test.ElasticsearchTestMode.NONE;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.auth.util.Auth0Utils;
import com.gentics.mesh.test.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@Ignore
@MeshTestSetting(elasticsearch = NONE, testSize = PROJECT_AND_NODE, startServer = true, useKeycloak = false)
public class OAuth2Auth0IntegrationTest extends AbstractOAuthTest {

	@Before
	public void deployPlugin() throws Exception {
		MapperTestPlugin.reset();
		setupPlugin();
		deployPlugin(MapperTestPlugin.class, "myMapper");
	}

	private void setupPlugin() throws Exception {
		Set<JsonObject> keys = Auth0Utils.loadJWKs("jotschi");
		MapperTestPlugin.publicKeys.addAll(keys);
		MapperTestPlugin.usernameExtractor = (token) -> {
			return Optional.of(token.getString("nickname"));
		};
	}

	@Test
	public void testAuth0() throws IOException {
		String clientId = "MWGBzxK1DgoU3Sqfk3NzltiS71o3OmQm";
		String username = "spam@jotschi.de";
		String password = "geheim";
		String clientSecret = "geheim";
		JsonObject token = Auth0Utils.loginAuth0("jotschi", clientId, username, password,
			clientSecret);
		System.out.println(token.encodePrettily());
		String accessToken = token.getString("id_token");
		client().setAPIKey(accessToken);
		System.out.println(client().me().blockingGet().toJson());
	}
}
