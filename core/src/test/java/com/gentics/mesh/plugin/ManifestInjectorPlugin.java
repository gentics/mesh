package com.gentics.mesh.plugin;

import com.gentics.mesh.core.rest.plugin.PluginManifest;

import io.vertx.ext.web.Router;

/**
 * A plugin which allows the injection of manifest data in order to test manifest validation.
 */
public class ManifestInjectorPlugin extends AbstractPluginVerticle {

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
