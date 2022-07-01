package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.JWTTestUtil;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.jwt.JWTOptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = PROJECT, startServer = true, loginClients = false)
public class JWTPayloadValidationTest extends AbstractMeshTest {

	private static final Consumer<MeshOptions> NULL_PROVIDER = meshOptions -> {
		meshOptions.getAuthenticationOptions()
			.setIssuer(null)
			.setAudience(null);
	};

	private static final Consumer<MeshOptions> META_PROVIDER = meshOptions -> {
		meshOptions.getAuthenticationOptions()
			.setIssuer("HELLO WORLD")
			.setAudience(Arrays.asList("www.example.com", "auth.example.com"));
	};

	@Parameterized.Parameters()
	public static Collection<Object[]> paramData() {
		return Arrays.asList(new Object[][]{
			{    // Server and client send minimal token payload
				NULL_PROVIDER,
				NULL_PROVIDER,
				true
			},
			{    // Server sends Issuer & Audience, but client sends none
				META_PROVIDER,
				NULL_PROVIDER,
				false
			},
			{    // Server sends no Issuer or Audience, but client does
				NULL_PROVIDER,
				META_PROVIDER,
				true,
			},
			{    // Server and Client send meta-data
				META_PROVIDER,
				META_PROVIDER,
				true
			}
		});
	}

	@Parameterized.Parameter(0)
	public Consumer<MeshOptions> serverOptionsChanger;

	@Parameterized.Parameter(1)
	public Consumer<MeshOptions> clientOptionsChanger;

	@Parameterized.Parameter(2)
	public Boolean shouldBeValid;

	private MeshOptions originalOptions;

	@Before
	public void backupOptions() {
		if (this.originalOptions == null) {
			this.originalOptions = JsonUtil.readValue(JsonUtil.toJson(this.options()), MeshOptions.class);
		}
	}

	@Test
	public void testOptionChanges() throws IOException {
		try (Tx tx = tx()) {
			MeshOptions clientOptions = JsonUtil.readValue(JsonUtil.toJson(this.originalOptions), MeshOptions.class);
			this.serverOptionsChanger.accept(this.options());
			this.clientOptionsChanger.accept(clientOptions);

			Tuple<JWTAuth, JWTOptions> jwt = JWTTestUtil.createAuth(vertx(), clientOptions, null);
			User user = user();
			JsonObject tokenData = new JsonObject()
				.put("userUuid", user.getUuid());
			String token = jwt.v1().generateToken(tokenData, jwt.v2());

			MeshRestClient client = MeshRestClient.create("localhost", port(), false);
			client.getAuthentication().setToken(token);

			try {
				UserResponse res = client.me().blockingGet();
				assertEquals(user.getUuid(), res.getUuid());
				assertEquals(shouldBeValid, true);
			} catch (Exception e) {
				assertEquals(shouldBeValid, false);
			}
		}
	}
}
