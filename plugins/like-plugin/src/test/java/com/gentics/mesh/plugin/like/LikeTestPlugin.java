package com.gentics.mesh.plugin.like;

import java.io.File;
import java.net.URISyntaxException;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

/**
 * Test plugin, the configuration is loaded from the file "config.yml" in the classpath
 */
public class LikeTestPlugin extends LikePlugin {

	public LikeTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	protected File getConfigFile() {
		try {
			return new File(LikePluginIntegrationTest.class.getResource("config.yml").toURI());
		} catch (URISyntaxException e) {
			return null;
		}
	};
}
