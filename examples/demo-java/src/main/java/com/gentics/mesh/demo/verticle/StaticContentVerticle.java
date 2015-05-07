package com.gentics.mesh.demo.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.apex.Router;
import io.vertx.ext.apex.handler.StaticHandler;
import io.vertx.ext.apex.handler.TemplateHandler;
import io.vertx.ext.apex.templ.HandlebarsTemplateEngine;
import io.vertx.ext.apex.templ.TemplateEngine;

public class StaticContentVerticle extends AbstractVerticle {

	@Override
	public void start() throws Exception {

		Router router = Router.router(vertx);
		StaticHandler staticHandler = StaticHandler.create();

		router.route("/js").handler(staticHandler);
		router.route("/img").handler(staticHandler);
		router.route("/css").handler(staticHandler);

		// All other requests handled by template engine
		TemplateEngine engine = HandlebarsTemplateEngine.create();

		// // Example content
		router.route("/test.html").handler(context -> {
			context.put("mesh.page.id", "1");
			context.put("mesh.page.title", "My title");
			context.put("mesh.page.teaser", "My teaser");
			context.put("mesh.page.content", "My content");
			context.next();
		});
		router.route("/test2.html").handler(context -> {
			context.put("mesh.page.id", "1");
			context.put("mesh.page.title", "My title");
			context.put("mesh.page.teaser", "My teaser");
			context.put("mesh.page.content", "My content");
			context.next();
		});

		router.route().handler(TemplateHandler.create(engine, "templates/post", "text/html"));
		HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8081));
		server.requestHandler(router::accept);
		server.listen();

	}
}
