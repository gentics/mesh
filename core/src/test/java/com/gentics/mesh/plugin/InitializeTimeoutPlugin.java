package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.reactivex.Completable;

/**
 * Plugin that will fail the initialisation due to timeout.
 */
public class InitializeTimeoutPlugin extends AbstractPlugin {

	public InitializeTimeoutPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		return Completable.create(sub -> {
			Thread.sleep(4000);
			sub.onComplete();
		});
	}

}
