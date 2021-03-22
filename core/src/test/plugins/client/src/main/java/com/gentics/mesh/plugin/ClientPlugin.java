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
	public Router createGlobalRouter() {
		Router router = Router.router(vertx());
		router.route("/me").handler(rc -> {
			PluginContext context = wrap(rc);
			context.client().me().toSingle().subscribe(me -> {
				rc.response().end(me.toJson());
			}, rc::fail);
		});

		router.route("/user").handler(rc -> {
			rc.response().end(rc.user().principal().encodePrettily());
		});

		router.route("/admin").handler(rc -> {
			adminClient().me().toSingle().subscribe(me -> {
				rc.response().end(me.toJson());
			}, rc::fail);
		});
		return router;
	}

	@Override
	public Router createProjectRouter() {
		Router router = Router.router(vertx());
		router.route("/project").handler(rc -> {
			PluginContext context = wrap(rc);
			context.client().findProjectByName(context.project().getString("name")).toSingle().subscribe(project -> {
				rc.response().end(project.toJson());
			}, rc::fail);
		});
		return router;
	}

	@Override
	public String restApiName() {
		return "client";
	}

}
