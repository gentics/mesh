package com.gentics.mesh.plugin.formgenerator;

import java.io.File;
import java.net.URISyntaxException;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

/**
 * Tested plugin, the configuration is loaded from the file "config.yml" in the classpath
 */
public class FormGeneratorTestPlugin extends FormGeneratorPlugin {

	public FormGeneratorTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	protected File getConfigFile() {
		try {
			return new File(AbstractFormGeneratorPluginIntegrationTest.class.getResource("config.yml").toURI());
		} catch (URISyntaxException e) {
			return null;
		}
	};

}
