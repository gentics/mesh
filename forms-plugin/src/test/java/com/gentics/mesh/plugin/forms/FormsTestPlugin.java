package com.gentics.mesh.plugin.forms;

import java.io.File;
import java.net.URISyntaxException;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.forms.FormsPlugin;

/**
 * Tested plugin, the configuration is loaded from the file "config.yml" in the classpath
 */
public class FormsTestPlugin extends FormsPlugin {

	public FormsTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	protected File getConfigFile() {
		try {
			return new File(AbstractFormsPluginIntegrationTest.class.getResource("config.yml").toURI());
		} catch (URISyntaxException e) {
			return null;
		}
	};

}
