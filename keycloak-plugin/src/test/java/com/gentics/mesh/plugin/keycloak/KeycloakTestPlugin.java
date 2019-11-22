package com.gentics.mesh.plugin.keycloak;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.keycloak.KeycloakPlugin;
import com.gentics.mesh.plugin.keycloak.config.KeycloakPluginConfig;

/**
 * The test plugin overrides the actual plugin class and overrides the {@link #getConfig()} method so that the config can be injected without the use of the
 * filesystem.
 */
public class KeycloakTestPlugin extends KeycloakPlugin {

	public static KeycloakPluginConfig config;

	public KeycloakTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public KeycloakPluginConfig getConfig() {
		return config;
	}

}
