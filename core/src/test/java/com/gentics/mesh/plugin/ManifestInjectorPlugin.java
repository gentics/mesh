package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.ext.web.Router;

/**
 * A plugin which allows the injection of manifest data in order to test manifest validation.
 */
public class ManifestInjectorPlugin extends AbstractPlugin implements RestPlugin {

	public ManifestInjectorPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	public static PluginManifest manifest;

	public static String apiName;

	@Override
	public PluginManifest getManifest() {
		return manifest;
	}

	@Override
	public String apiName() {
		return apiName;
	}

	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {
		// TODO Auto-generated method stub

	}

}
