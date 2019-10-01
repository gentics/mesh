package com.gentics.mesh.plugin.favorite;

import java.io.File;
import java.net.URISyntaxException;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

/**
 * Test plugin, the configuration is loaded from the file "config.yml" in the classpath
 */
public class FavoriteIntegrationTestPlugin extends FavoritePlugin {

	public FavoriteIntegrationTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	protected File getConfigFile() {
		try {
			return new File(FavoritePluginIntegrationTest.class.getResource("config.yml").toURI());
		} catch (URISyntaxException e) {
			return null;
		}
	};
}
