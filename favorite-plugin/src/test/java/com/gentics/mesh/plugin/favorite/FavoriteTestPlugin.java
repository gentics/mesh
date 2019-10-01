package com.gentics.mesh.plugin.favorite;

import java.io.File;
import java.net.URISyntaxException;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

public class FavoriteTestPlugin extends FavoritePlugin {

	public FavoriteTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	protected File getConfigFile() {
		try {
			return new File(FavoriteTestPlugin.class.getResource("/test.yml").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
