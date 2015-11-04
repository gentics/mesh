package com.gentics.mesh.verticle.admin;

import java.util.Properties;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractSpringVerticle;

import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

@Component
@Scope("singleton")
@SpringVerticle
public class AdminGUIVerticle extends AbstractSpringVerticle {

	private static final Logger log = LoggerFactory.getLogger(AdminGUIVerticle.class);
	protected HttpServer server;

	//TODO handle NPEs
	private static String meshAdminUiVersion = readBuildProperties().getProperty("mesh.admin-ui.version");

	private static Properties readBuildProperties() {
		try {
			Properties buildProperties = new Properties();
			buildProperties.load(AdminGUIVerticle.class.getResourceAsStream("/mesh-admin-gui.properties"));
			return buildProperties;
		} catch (Exception e) {
			log.error("Error while loading build properties", e);
			return null;
		}
	}

	@Override
	public void start() throws Exception {

		Router staticRouter = Router.router(vertx);
		staticRouter.route("/*").handler(StaticHandler.create("META-INF/resources/webjars/mesh-ui/" + meshAdminUiVersion).setIndexPage("index.html"));
		routerStorage.getRootRouter().mountSubRouter("/mesh-ui", staticRouter);

		routerStorage.getRootRouter().route("/").handler(rc -> {
			rc.response().setStatusCode(302);
			rc.response().headers().set("Location", "/mesh-ui/");
			rc.response().end();
		});
		server = vertx.createHttpServer(new HttpServerOptions().setPort(config().getInteger("port")));
		server.requestHandler(routerStorage.getRootRouter()::accept);
		server.listen();

		// StaticServer webJarServer = StaticServer.staticServer("META-INF/resources/webjars");
		// StaticServer staticContentServer = StaticServer.staticServer();
		// router.route("/angularjs").handler(webJarServer);
		// router.route().handler(staticContentServer);

		// // All other requests handled by template engine
		//		 TemplateEngine engine = HandlebarsTemplateEngine.create();
		//		 engine.render(context, templateFileName, handler);
		//		 Handlebars handlebars;
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
		// HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8081));
		// server.requestHandler(router::accept);
		// server.listen();

	}

}
