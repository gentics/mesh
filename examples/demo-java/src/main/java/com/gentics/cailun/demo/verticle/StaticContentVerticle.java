package com.gentics.cailun.demo.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.apex.addons.StaticServer;
import io.vertx.ext.apex.addons.TemplateEngine;
import io.vertx.ext.apex.addons.ThymeleafTemplateEngine;
import io.vertx.ext.apex.core.Router;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class StaticContentVerticle extends AbstractVerticle {

	@Override
	public void start() throws Exception {

		Router router = Router.router(vertx);

		 StaticServer staticServer = StaticServer.staticServer();
//		 staticServer.setDirectoryListing(true);

		// 1. Handle static resources
		 router.route().handler(staticServer);

		TemplateEngine engine = ThymeleafTemplateEngine.create();

		// All other requests handled by template engine
		//router.handler(TemplateHandler.templateHandler(engine, "templates/post.html", "text/html"));

		// Example content
		router.route("/test.html").handler(context -> {
			context.put("cailun.page.id", "1");
			context.put("cailun.page.title", "My title");
			context.put("cailun.page.teaser", "My teaser");
			context.put("cailun.page.content", "My content");
			context.next();
		});

		HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8081));
		server.requestHandler(router::accept);
		server.listen();

	}

}
