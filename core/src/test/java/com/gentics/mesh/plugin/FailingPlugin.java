package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import io.reactivex.Completable;
import io.vertx.ext.web.Router;

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

	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {
	}
}
