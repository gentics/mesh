package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.util.JWTUtil;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = PROJECT, startServer = true, loginClients = false)
public class JWTPayloadTest extends AbstractMeshTest {

	@Parameter(0)
	public String paramGroupName;

	@Parameter(1)
	public Consumer<MeshOptions> serverOptionsChanger;

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<Object[]> paramData() {
		return Arrays.asList(new Object[][]{
			{
				"Issuer: HELLO WORLD",
				(Consumer<MeshOptions>) meshOptions -> meshOptions.getAuthenticationOptions()
					.setIssuer("HELLO WORLD")
			},
			{
				"Audience: [foo, bar]",
				(Consumer<MeshOptions>) meshOptions -> meshOptions.getAuthenticationOptions()
					.setAudience(Arrays.asList("foo", "bar"))
			},
			{
				"Issuer: HELLO WORLD, Audience: [foo, bar]",
				(Consumer<MeshOptions>) meshOptions -> meshOptions.getAuthenticationOptions()
					.setIssuer("HELLO WORLD")
					.setAudience(Arrays.asList("foo", "bar"))
			},
		});
	}

	@Before
	public void setOptions() {
		serverOptionsChanger.accept(testContext.getOptions());
	}

	@Test
	public void testJWTPayload() throws IOException {
		try (Tx tx = tx()) {
			this.serverOptionsChanger.accept(this.options());

			HibUser user = user();
			String username = user.getUsername();

			MeshRestClient client = MeshRestClient.create("localhost", port(), false);
			client.setLogin(username, data().getUserInfo().getPassword());

			Single<GenericMessageResponse> future = client.login();

			GenericMessageResponse loginResponse = future.blockingGet();
			assertNotNull(loginResponse);
			assertEquals("OK", loginResponse.getMessage());

			String token = client.getAuthentication().getToken();
			assertNotNull(token);

			JsonObject payload = new JsonObject(new String(Base64.getDecoder().decode(token.split("\\.")[1])));
			assertEquals(payload.getString(JWTUtil.JWT_FIELD_ISSUER), options().getAuthenticationOptions().getIssuer());
			JsonArray aud = null;
			if (options().getAuthenticationOptions().getAudience() != null && !options().getAuthenticationOptions().getAudience().isEmpty()) {
				aud = new JsonArray(options().getAuthenticationOptions().getAudience());
			}
			assertEquals(payload.getJsonArray(JWTUtil.JWT_FIELD_AUDIENCE), aud);
		}
	}
}
