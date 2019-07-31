package com.gentics.mesh.plugin;

import java.util.concurrent.atomic.AtomicInteger;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.reactivex.Completable;
import io.vertx.ext.web.Router;

/**
 * A plugin which fakes the manifest in order to be deployable multiple times. This is useful to test deployments of multiple plugins.
 */
public class ClonePlugin extends AbstractPlugin implements RestPlugin {

	public ClonePlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	public static AtomicInteger counter = new AtomicInteger(1);

	private Integer myCount = null;

	@Override
	public PluginManifest getManifest() {
		PluginManifest manifest = super.getManifest();
		manifest.setName("Clone Plugin " + myCount);
		manifest.setAuthor("Johannes Schüth");
		manifest.setLicense("Apache License 2.0");
		manifest.setVersion("1.0");
		manifest.setDescription("A very dummy plugin for tests");
		manifest.setInception("26-04-2018");
		return manifest;
	}

	@Override
	public Completable initialize() {
		if (myCount == null) {
			myCount = counter.getAndIncrement();
		}
		return Completable.complete();
	}

	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {
		globalRouter.route("/hello").handler(rc -> {
			rc.response().end("world");
		});

		projectRouter.route("/hello").handler(rc -> {
			rc.response().end("project");
		});

		globalRouter.route("/manifest").handler(rc -> {
			rc.response().end(JsonUtil.toJson(getManifest()));
		});
	}

	@Override
	public String apiName() {
		return "clone" + myCount;
	}
}
