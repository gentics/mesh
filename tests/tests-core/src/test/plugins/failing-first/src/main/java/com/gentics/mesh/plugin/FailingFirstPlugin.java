package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.reactivex.Completable;

public class FailingFirstPlugin extends AbstractPlugin implements RestPlugin {
	
	private static final Logger log = LoggerFactory.getLogger(FailingFirstPlugin.class);
	
	private static final String FILENAME = "target/already-failed.bin";

	public FailingFirstPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		return Completable.defer(() -> {
			try {
				log.info("Failing starts up!");
				environment().vertx().fileSystem().readFileBlocking(FILENAME);
				return Completable.complete();
			} catch (Exception e) {
				log.error("Failing failed!", e);
				environment().vertx().fileSystem().createFileBlocking(FILENAME);
				return Completable.error(e);
			}
		});
	}
}
