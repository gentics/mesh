package com.gentics.mesh.plugin;

import java.util.concurrent.atomic.AtomicInteger;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.core.rest.plugin.PluginManifest;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.ext.web.Router;

/**
 * A plugin which fakes the manifest in order to be deployable multiple times. This is useful to test deployments of multiple plugins.
 */
public class ClonePlugin extends AbstractPlugin {

	public ClonePlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	public static AtomicInteger counter = new AtomicInteger(0);

	private Integer myCount = null;

	private String uuid;

	@Override
	public PluginManifest getManifest() {
		if (myCount == null) {
			myCount = counter.incrementAndGet();
		}
		PluginManifest manifest = super.getManifest();
		manifest.setApiName("clone" + myCount);
		manifest.setName("Clone Plugin " + myCount);
		return manifest;
	}

	@Override
	public String deploymentID() {
		if (uuid != null) {
			return uuid;
		}
		return super.deploymentID();
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
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

}
