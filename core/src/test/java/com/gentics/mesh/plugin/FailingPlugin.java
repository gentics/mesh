package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import io.reactivex.Completable;

/**
 * Plugin that will fail the initialization
 */
public class FailingPlugin extends AbstractPlugin {

	public FailingPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Override
	public Completable initialize() {
		return Completable.error(new Exception("This must fail"));
	}

}
