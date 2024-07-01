package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionProviderPlugin extends AbstractPlugin {

	private static final Logger log = LoggerFactory.getLogger(ExtensionProviderPlugin.class);

	public ExtensionProviderPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		return Completable.fromAction(() -> {
			log.info("Extension provider plugin started");
		});
	}

	@Override
	public Completable shutdown() {
		return Completable.fromAction(() -> {
			log.info("Extension provider plugin stopped");
		});
	}

}
