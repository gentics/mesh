package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.MeshTestHelper;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.JWTUtil;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jwt.JWTOptions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = PROJECT, startServer = true)
public class JWTConfigurationTest extends AbstractMeshTest {

	@Parameterized.Parameter(0)
	public Consumer<MeshOptions> optionChanger;

	@Parameterized.Parameter(1)
	public Consumer<JWTOptions> jwtChanger;

	@Parameterized.Parameters(name = "{index}: authOptions={0}")
	public static Collection<Object[]> paramData() {
		return Arrays.asList(new Object[][] {
			{
				MeshTestHelper.noopConsumer(),
				(Consumer<JWTOptions>) token -> token
					.setIssuer("random 123")
					.setAudience(Arrays.asList("foo", "bar"))
			},
			{
				(Consumer<MeshOptions>) meshOptions -> {
					meshOptions.getAuthenticationOptions()
						.setIssuer("HELLO WORLD")
						.setAudience(Arrays.asList("www.example.com", "auth.example.com"));
				},
				(Consumer<JWTOptions>) token -> token
					.setIssuer("random 123")
					.setAudience(Arrays.asList("foo", "bar"))
			}
		});
	}

	@Override
	public MeshTestContext getTestContext() {
		return testContext.setOptionChanger(meshOptions -> {
			optionChanger.accept(meshOptions);
		});
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
			assertEquals(payload.getString(JWTUtil.JWT_FIELD_ISSUER), options.getAuthenticationOptions().getIssuer());
			JsonArray aud = new JsonArray();
			if (options.getAuthenticationOptions().getAudience() != null) {
				aud = new JsonArray(options.getAuthenticationOptions().getAudience());
			}
			assertEquals(payload.getJsonArray(JWTUtil.JWT_FIELD_AUDIENCE), aud);
		}
	}

	@Test
	public void testOptionChanges() throws IOException {
		try (Tx tx = tx()) {
			MeshOptions options = this.options();
			this.optionChanger.accept(options);

			String keyStorePath = options.getAuthenticationOptions().getKeystorePath();
			String keystorePassword = options.getAuthenticationOptions().getKeystorePassword();
			String type = "jceks";
			JWTAuthOptions config = new JWTAuthOptions();
			JWTOptions jwtOptions = JWTUtil.createJWTOptions(options.getAuthenticationOptions());
			this.jwtChanger.accept(jwtOptions);

			// Set JWT options from the config
			config.setJWTOptions(jwtOptions);
			config.setKeyStore(new KeyStoreOptions().setPath(keyStorePath).setPassword(keystorePassword).setType(type));
			JWTAuth jwtProvider = JWTAuth.create(vertx(), new JWTAuthOptions(config));
			User user = user();
			JsonObject tokenData = new JsonObject()
				.put("userUuid", user.getUuid());
			String token = jwtProvider.generateToken(tokenData, jwtOptions);

			MeshRestClient client = MeshRestClient.create("localhost", port(), false);
			client.getAuthentication().setToken(token);

			try {
				UserResponse res = client.me().blockingGet();
				assertEquals(user.getUuid(), res.getUuid());
			} catch (Exception e) {
				assertTrue(false);
			}
		}
	}
}
