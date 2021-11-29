package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.function.Consumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.MeshTestHelper;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = PROJECT, startServer = true)
public class JWTConfigurationTest extends AbstractMeshTest {

	@Parameterized.Parameter(0)
	public Consumer<MeshOptions> optionChanger;

	@Parameterized.Parameters(name = "{index}: authOptions={0}")
	public static Collection<Object[]> paramData() {
		return Arrays.asList(new Object[][] {
			{ MeshTestHelper.noopConsumer() },
			{(Consumer<MeshOptions>) meshOptions -> {
				meshOptions.getAuthenticationOptions()
					.setIssuer("HELLO WORLD")
					.setAudience(Arrays.asList("www.example.com", "auth.example.com"));
			}}
		});
	}

	@Override
	public MeshTestContext getTestContext() {
		return testContext.setOptionChanger(optionChanger);
	}

	@Test
	public void testJWTPayload() throws IOException {
		try (Tx tx = tx()) {
			MeshOptions options = this.options();
			this.optionChanger.accept(options);

			User user = user();
			String username = user.getUsername();

			MeshRestClient client = MeshRestClient.create("localhost", port(), false);
			client.setLogin(username, data().getUserInfo().getPassword());

			Single<GenericMessageResponse> future = client.login();

			GenericMessageResponse loginResponse = future.blockingGet();
			assertNotNull(loginResponse);
			assertEquals("OK", loginResponse.getMessage());

			String tokenCookie = client.me().getResponse().blockingGet().getHeader("Set-Cookie").orElse(null);
			assertNotNull(tokenCookie);
			String token = tokenCookie.substring(11, tokenCookie.indexOf(';'));
			assertNotNull(token);

			JsonObject payload = new JsonObject(new String(Base64.getDecoder().decode(token.split("\\.")[1])));
			assertEquals(options.getAuthenticationOptions().getIssuer(), payload.getString("iss"));
			if (options.getAuthenticationOptions().getAudience() != null) {
				assertEquals(new JsonArray(options.getAuthenticationOptions().getAudience()), payload.getJsonArray("aud"));
			} else {
				assertNull(payload.getJsonArray("aud"));
			}
		}
	}
}
