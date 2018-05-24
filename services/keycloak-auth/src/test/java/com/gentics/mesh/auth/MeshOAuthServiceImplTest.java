package com.gentics.mesh.auth;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.script.ScriptException;

import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class MeshOAuthServiceImplTest {

	@Test
	public void testPrincipleMapper() throws ScriptException {
		File scriptFile = new File("src/test/resources/dummyscript.js");
		MeshOptions meshOptions = new MeshOptions();

		SearchQueue searchQueue = Mockito.mock(SearchQueue.class);

		MeshOAuthServiceImpl service = new MeshOAuthServiceImpl(null, null, meshOptions, Vertx.vertx(), searchQueue);
		service.options.setMapperScriptPath(scriptFile.getAbsolutePath());
		String script = service.loadScript();
		assertNotNull(script);

		service.mapperScript = script;
		JsonObject json = new JsonObject();
		json.put("name", "blub");
		JsonObject mappedJson = service.executeMapperScript(json);
		assertTrue(mappedJson.getJsonArray("roles").contains("role1"));
		assertTrue(mappedJson.getJsonArray("roles").contains("role2"));
		assertTrue(mappedJson.getJsonArray("groups").contains("group1"));
		assertTrue(mappedJson.getJsonArray("groups").contains("group2"));
	}
}
