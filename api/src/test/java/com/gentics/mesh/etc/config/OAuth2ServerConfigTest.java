package com.gentics.mesh.etc.config;

import static org.junit.Assert.assertEquals;

import io.vertx.core.json.JsonObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class OAuth2ServerConfigTest {

	@Rule
	public EnvironmentVariables environmentVariables = new EnvironmentVariables();

	@Test
	public void testCredentialsOverride() {
		String valueSetDirectly = "value set directly";
		String valueSetViaEnv = "value set via environment";
		String key = "mykey";
		JsonObject json = new JsonObject();
		json.put(key, valueSetViaEnv);
		environmentVariables.set(OAuth2ServerConfig.MESH_AUTH_OAUTH2_SERVER_CONF_CREDENTIALS, json.encode());
		OAuth2ServerConfig conf = new OAuth2ServerConfig();
		conf.addCredential(key, valueSetDirectly);
		assertEquals("The credentials map should contain the key we set directly", valueSetDirectly,
				conf.getCredentials().get(key));
		conf.overrideWithEnv();
		assertEquals("The credentials map should contain the key we have overridden vie env vars", valueSetViaEnv,
				conf.getCredentials().get(key));
	}
}
