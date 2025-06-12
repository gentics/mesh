package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.JWTTestUtil;
import com.gentics.mesh.util.JWTUtil;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = PROJECT, startServer = true, loginClients = false)
public class JWTLeewayTimeoutValidationTest extends AbstractMeshTest {

	private final Consumer<MeshOptions> APPLY_OPTIONS = meshOptions -> {
		meshOptions.getAuthenticationOptions()
			.setLeeway(this.leeway)
			.setIgnoreExpiration(false);
	};

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<Object[]> paramData() {
		return Arrays.asList(new Object[][]{
			{10, -5, true},
			{10, -20, false},
		});
	}

	@Parameterized.Parameter(0)
	public Integer leeway;

	@Parameterized.Parameter(1)
	public Integer offset;

	@Parameterized.Parameter(2)
	public Boolean shouldBeValid;

	@Test
	public void testTokenExpiration() throws IOException {
		try (Tx tx = tx()) {
			APPLY_OPTIONS.accept(this.options());

			Tuple<JWTAuth, JWTOptions> jwt = JWTTestUtil.createAuth(vertx(), this.options(), jwtOptions -> {
				// Set it to 0, because we set the expiration (exp) manually below
				jwtOptions.setExpiresInSeconds(0);
				// And disable the timestamp (iat)
				jwtOptions.setNoTimestamp(true);
			});

			HibUser user = user();
			JsonObject tokenData = new JsonObject()
				.put("userUuid", user.getUuid())
				.put(JWTUtil.JWT_FIELD_EXPIRATION, (System.currentTimeMillis() / 1000L) + this.offset);
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
			assertEquals(isValid, this.shouldBeValid);
		}
	}
}
