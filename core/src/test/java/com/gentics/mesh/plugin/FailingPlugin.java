package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.reactivex.Completable;

/**
 * Plugin that will fail the initialisation
 */
public class FailingPlugin extends AbstractPlugin {

	public FailingPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		return Completable.error(new Exception("This must fail"));
	}

}
