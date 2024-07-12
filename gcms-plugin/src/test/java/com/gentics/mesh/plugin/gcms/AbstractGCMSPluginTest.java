package com.gentics.mesh.plugin.gcms;

import com.gentics.mesh.plugin.gcms.config.GCMSPluginConfig;
import com.gentics.mesh.plugin.test.common.AbstractPluginLocalIntegrationTest;
import com.gentics.mesh.plugin.test.common.HibernateMeshLocalServer;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import com.gentics.mesh.test.local.MeshLocalServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Test cases for the GCMS plugin
 */
public abstract class AbstractGCMSPluginTest extends AbstractPluginLocalIntegrationTest {

	@BeforeClass
	public static void configurePlugin() throws IOException {
		GCMSTestPlugin.config = new GCMSPluginConfig();
		GCMSTestPlugin.publicKey = new JsonObject(IOUtils.toString(AbstractGCMSPluginTest.class.getResource("/public-key.jwk"), "UTF-8"));
	}

	@Rule
	public final MeshLocalServer server = new HibernateMeshLocalServer()
		.withPlugin(GCMSTestPlugin.class, "gcmsPlugin")
		.waitForStartup();

	/**
	 * JWT Provider
	 */
	protected JWTAuth provider;

	@Before
	public void createAuthProvider() throws IOException {
		provider = JWTAuth.create(server.getMesh().getVertx(), new JWTAuthOptions().addJwk(
				new JsonObject(IOUtils.toString(AbstractGCMSPluginTest.class.getResource("/private-key.jwk"), "UTF-8"))));
	}

	@Before
	public void setupPlugin() throws InterruptedException {
		PluginTestUtil.waitForPluginRegistration(server, 5_000, "gcmsPlugin");
	}

	/**
	 * Get signed token
	 * @param username username
	 * @param firstname first name
	 * @param lastname last name
	 * @param email email address
	 * @param groupNames group names
	 * @return signed token
	 */
	protected String getToken(String username, String firstname, String lastname, String email,
			List<String> groupNames) {
		return provider.generateToken(
			// Make sure that the added claims are the same that the CMS
			// actually sets in ProxyResource#forward(T method, Consumer<T> prepareHandler).
			new JsonObject()
				.put("preferred_username", username)
				.put("given_name", firstname)
				.put("family_name", lastname)
				.put("email", email)
				.put("gcms_groups", groupNames)
				.put("iss", "Gentics CMS")
				.put("iat", OffsetDateTime.now().toEpochSecond()),
			new JWTOptions().setAlgorithm("RS256"));
	}
}
