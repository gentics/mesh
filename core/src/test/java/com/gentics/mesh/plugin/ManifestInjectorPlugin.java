package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.core.rest.plugin.PluginManifest;

import io.vertx.ext.web.Router;

/**
 * A plugin which allows the injection of manifest data in order to test manifest validation.
 */
public class ManifestInjectorPlugin extends AbstractPlugin {

	public ManifestInjectorPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	
	public static PluginManifest manifest;

	@Override
	public PluginManifest getManifest() {
		return manifest;
	}

	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {
		// NOOP
	}

}
