package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.reactivex.Completable;

/**
 * Plugin that will fail the stop
 */
public class FailingShutdownPlugin extends AbstractPlugin {

	public FailingShutdownPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable shutdown() {
		return Completable.error(new RuntimeException("Shutdown failure"));
	}

}
