package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.JWTUtil;
import com.gentics.mesh.util.Tuple;

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
@MeshTestSetting(testSize = PROJECT, startServer = true, loginClients = false)
public class JWTConfigurationTest extends AbstractMeshTest {

	private static class LeewayOptions {
		int leeway;
		int offset;
		boolean valid;

		public LeewayOptions(int leeway, int offset, boolean valid) {
			this.leeway = leeway;
			this.offset = offset;
			this.valid = valid;
		}

		@Override
		public String toString() {
			return "LeewayOptions{" +
				"leeway=" + leeway +
				", offset=" + offset +
				", valid=" + valid +
				'}';
		}
	}

	@Parameterized.Parameter(0)
	public BiConsumer<MeshOptions, LeewayOptions> serverOptionsChanger;

	@Parameterized.Parameter(1)
	public BiConsumer<MeshOptions, LeewayOptions> clientOptionsChanger;

	@Parameterized.Parameter(2)
	public Consumer<JWTOptions> jwtChanger;

	@Parameterized.Parameter(3)
	public LeewayOptions leewayOptions;

	private static BiConsumer<MeshOptions, LeewayOptions> NOOP_CHANGER = (meshOptions, leeway) -> {};
	private static BiConsumer<MeshOptions, LeewayOptions> LEEWAY_ONLY = (meshOptions, leeway) -> {
		meshOptions.getAuthenticationOptions()
			.setLeeway(leeway.leeway);
	};

	@Parameterized.Parameters(name = "{index}: {3}")
	public static Collection<Object[]> paramData() {
		return Arrays.asList(new Object[][] {
			{
				LEEWAY_ONLY,
				LEEWAY_ONLY,
				(Consumer<JWTOptions>) token -> token
					.setIssuer("random 123")
					.setAudience(Arrays.asList("foo", "bar")),
				new LeewayOptions(10, -5, true)
			},
			{
				(BiConsumer<MeshOptions, LeewayOptions>) (meshOptions, leeway) -> {
					meshOptions.getAuthenticationOptions()
						.setLeeway(leeway.leeway)
						.setIssuer("HELLO WORLD")
						.setAudience(Arrays.asList("www.example.com", "auth.example.com"));
				},
				LEEWAY_ONLY,
				(Consumer<JWTOptions>) token -> token
					.setIssuer("random 123")
					.setAudience(Arrays.asList("foo", "bar")),
				new LeewayOptions(10, -20, false)
			}
		});
	}

	@Override
	public MeshTestContext getTestContext() {
		return testContext.setOptionChanger(meshOptions -> {
			serverOptionsChanger.accept(meshOptions, this.leewayOptions);
		});
	}

	private Tuple<JWTAuth, JWTOptions> createAuth(MeshOptions options, Consumer<JWTOptions> jwtChanger) {
		String keyStorePath = options.getAuthenticationOptions().getKeystorePath();
		String keystorePassword = options.getAuthenticationOptions().getKeystorePassword();
		String type = "jceks";
		JWTAuthOptions config = new JWTAuthOptions();
		JWTOptions jwtOptions = JWTUtil.createJWTOptions(options.getAuthenticationOptions());
		if (jwtChanger != null) {
			jwtChanger.accept(jwtOptions);
		}

		// Set JWT options from the config
		config.setJWTOptions(jwtOptions);
		config.setKeyStore(new KeyStoreOptions().setPath(keyStorePath).setPassword(keystorePassword).setType(type));
		JWTAuth jwtProvider = JWTAuth.create(vertx(), new JWTAuthOptions(config));

		return Tuple.tuple(jwtProvider, jwtOptions);
	}

	@Test
	public void testJWTPayload() throws IOException {
		try (Tx tx = tx()) {
			MeshOptions options = JsonUtil.readValue(JsonUtil.toJson(this.options()), MeshOptions.class);
			this.serverOptionsChanger.accept(this.options(), this.leewayOptions);

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
			MeshOptions options = JsonUtil.readValue(JsonUtil.toJson(this.options()), MeshOptions.class);
			this.serverOptionsChanger.accept(this.options(), this.leewayOptions);

			Tuple<JWTAuth, JWTOptions> jwt = this.createAuth(options, this.jwtChanger);
			User user = user();
			JsonObject tokenData = new JsonObject()
				.put("userUuid", user.getUuid());
			String token = jwt.v1().generateToken(tokenData, jwt.v2());

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

	@Test
	public void testLeewayExpiration() throws IOException {
		try (Tx tx = tx()) {
			MeshOptions options = JsonUtil.readValue(JsonUtil.toJson(this.options()), MeshOptions.class);
			this.serverOptionsChanger.accept(this.options(), this.leewayOptions);
			this.clientOptionsChanger.accept(options, this.leewayOptions);

			Tuple<JWTAuth, JWTOptions> jwt = this.createAuth(options, jwtOptions -> {
				this.jwtChanger.accept(jwtOptions);
				// Set it to 0, because we set the expiration manually below
				jwtOptions.setExpiresInSeconds(0);
				// And disable the timestamp
				jwtOptions.setNoTimestamp(true);
				jwtOptions.setLeeway(this.leewayOptions.leeway);
			});

			User user = user();
			JsonObject tokenData = new JsonObject()
				.put("userUuid", user.getUuid())
				.put("exp", (System.currentTimeMillis() / 1000L) + this.leewayOptions.offset);
			String token = jwt.v1().generateToken(tokenData, jwt.v2());

			MeshRestClient client = MeshRestClient.create("localhost", port(), false);
			client.getAuthentication().setToken(token);

			boolean isValid;
			try {
				client.me().blockingGet();
				isValid = true;
			} catch (Exception e) {
				isValid = false;
			}
			assertEquals(isValid, this.leewayOptions.valid);
		}
	}
}
