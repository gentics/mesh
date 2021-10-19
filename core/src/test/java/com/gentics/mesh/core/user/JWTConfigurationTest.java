package com.gentics.mesh.core.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestContext;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JWTConfigurationTest extends AbstractMeshTest {

	@Parameterized.Parameter(0)
	public AuthenticationOptions authOptions;

	@Parameterized.Parameters(name = "{index}: authOptions={0}")
	public static Collection<Object[]> paramData() {
		AuthenticationOptions defaultOptions = new AuthenticationOptions();
		defaultOptions.overrideWithEnv();

		return Arrays.asList(new Object[][] {
			{ defaultOptions },
			{ new AuthenticationOptions()
				.setIssuer("HELLO WORLD TEST")
				.setAudience(Arrays.asList("www.example.com", "auth.example.com"))
			}
		});
	}

	@Override
	public MeshTestContext getTestContext() {
		return testContext.setOptionChanger(options -> {
			options.setAuthenticationOptions(authOptions);
		});
	}

	@Test
	public void testJWTPayload() throws IOException {
		try (Tx tx = tx()) {
			User user = user();
			String username = user.getUsername();

			MeshRestClient client = MeshRestClient.create("localhost", port(), false);
			client.setLogin(username, data().getUserInfo().getPassword());

			Single<GenericMessageResponse> future = client.login();

			GenericMessageResponse loginResponse = future.blockingGet();
			assertNotNull(loginResponse);
			assertEquals("OK", loginResponse.getMessage());

			String token = client.me().getResponse().blockingGet().getHeader("Set-Cookie").orElse(null);
			assertNotNull(token);

			JsonObject payload = new JsonObject(new String(Base64.getDecoder().decode(token.split("\\.")[1])));
			assertEquals(payload.getString("iss"), authOptions.getIssuer());
			assertEquals(payload.getJsonArray("aud"), new JsonArray(authOptions.getAudience()));
		}
	}
}
