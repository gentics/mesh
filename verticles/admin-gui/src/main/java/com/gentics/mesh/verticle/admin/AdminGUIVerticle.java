package com.gentics.mesh.verticle.admin;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.apex.Router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminGUIVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(AdminGUIVerticle.class);

	@Override
	public void start() throws Exception {
		Router router = Router.router(vertx);

//		StaticServer webJarServer = StaticServer.staticServer("META-INF/resources/webjars");
//		StaticServer staticContentServer = StaticServer.staticServer();
//		router.route("/angularjs").handler(webJarServer);
//		router.route().handler(staticContentServer);

		// // All other requests handled by template engine
		// TemplateEngine engine = HandlebarsTemplateEngine.create();
		//
		// // // Example content
		// router.route("/test.html").handler(context -> {
		// context.put("mesh.page.id", "1");
		// context.put("mesh.page.title", "My title");
		// context.put("mesh.page.teaser", "My teaser");
		// context.put("mesh.page.content", "My content");
		// context.next();
		// });
		// router.route("/test2.html").handler(context -> {
		// context.put("mesh.page.id", "1");
		// context.put("mesh.page.title", "My title");
		// context.put("mesh.page.teaser", "My teaser");
		// context.put("mesh.page.content", "My content");
		// context.next();
		// });
		//
		// router.route().handler(TemplateHandler.templateHandler(engine, "templates/post", "text/html"));
		HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8081));
		server.requestHandler(router::accept);
		server.listen();

	}

}
