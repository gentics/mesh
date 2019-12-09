package com.gentics.mesh.plugin.keycloak;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.keycloak.KeycloakPlugin;
import com.gentics.mesh.plugin.keycloak.config.KeycloakPluginConfig;

import io.reactivex.Single;

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

	@Override
	public Single<KeycloakPluginConfig> loadConfig(Class<KeycloakPluginConfig> classOfT) {
		return Single.just(config);
	}

	@Override
	public String getMapperScript() {
		try {
			return IOUtils.toString(getClass().getResourceAsStream(DEFAULT_SCRIPT_RESOURCE));
		} catch (IOException e) {
			throw new RuntimeException("Could not load default config for test plugin");
		}
	}

}
