package com.gentics.mesh.plugin.keycloak;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.mockito.Mockito;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class GroovyTest {

	@Test
	public void testGroovy() {
		PluginWrapper wrapper = Mockito.mock(PluginWrapper.class);
		KeycloakTestPlugin plugin = new KeycloakTestPlugin(wrapper, null);
		String uuid = UUIDUtil.randomUUID();
		JsonObject token = new JsonObject();
		HttpServerRequest req = Mockito.mock(HttpServerRequest.class);
		MappingResult result = plugin.evaluateMapper(req, uuid, token);
		assertNotNull(result);
	}
}
