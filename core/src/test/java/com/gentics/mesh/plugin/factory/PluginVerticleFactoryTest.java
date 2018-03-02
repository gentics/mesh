package com.gentics.mesh.plugin.factory;

import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class PluginVerticleFactoryTest extends AbstractMeshTest {

	@Test
	public void testLocalDeployment() {
		//vertx().deployVerticle("plugin:com.gentics.mesh.plugin:hello-world:0.1.0-SNAPSHOT");
		//vertx().deployVerticle("plugin:hello-world-plugin"); 
		vertx().deployVerticle("plugin:../../mesh-hello-world-plugin");
	}

}
