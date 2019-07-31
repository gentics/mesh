package com.gentics.mesh.plugin;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

import io.vertx.ext.web.Router;

/**
 * Plugin which is used to test the interaction with the Gentics Mesh REST clients
 */
public class ClientPlugin extends AbstractPlugin implements RestPlugin {

	public ClientPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public PluginManifest getManifest() {
		PluginManifest manifest = new PluginManifest();
		manifest.setAuthor("Joe Doe");
		manifest.setId("client");
		manifest.setInception("2018");
		manifest.setVersion("1.0");
		manifest.setLicense("Apache License 2.0");
		manifest.setName("Client Test Plugin");
		manifest.setDescription("Plugin to test client interaction");
		return manifest;
	}

	@Override
	public void registerEndpoints(Router globalRouter, Router projectRouter) {

		globalRouter.route("/me").handler(rc -> {
			PluginContext context = wrap(rc);
			context.client().me().toSingle().subscribe(me -> {
				rc.response().end(me.toJson());
			}, rc::fail);
		});

		globalRouter.route("/user").handler(rc -> {
			rc.response().end(rc.user().principal().encodePrettily());
		});

		globalRouter.route("/admin").handler(rc -> {
			adminClient().me().toSingle().subscribe(me -> {
				rc.response().end(me.toJson());
			}, rc::fail);
		});

		projectRouter.route("/project").handler(rc -> {
			PluginContext context = wrap(rc);
			context.client().findProjectByName(context.project().getString("name")).toSingle().subscribe(project -> {
				rc.response().end(project.toJson());
			}, rc::fail);
		});
	}

	@Override
	public String apiName() {
		return "client";
	}

}
