package com.gentics.mesh.verticle.admin;

import static io.vertx.core.http.HttpMethod.GET;

import java.util.Properties;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractWebVerticle;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

@Component
@Scope("singleton")
@SpringVerticle
public class AdminGUIVerticle extends AbstractWebVerticle {

	private static final Logger log = LoggerFactory.getLogger(AdminGUIVerticle.class);

	//TODO handle NPEs
	private static String meshAdminUiVersion = readBuildProperties().getProperty("mesh.admin-ui.version");

	public AdminGUIVerticle() {
		super("mesh-ui");
	}

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

	private void addRedirectionHandler() {
		routerStorage.getRootRouter().route("/").method(GET).handler(rc -> {
			rc.response().setStatusCode(302);
			rc.response().headers().set("Location", "/" + basePath + "/");
			rc.response().end();
		});
	}

	private void addMeshUiStaticHandler() {
		route("/*").method(GET).handler(StaticHandler.create("META-INF/resources/webjars/mesh-ui/" + meshAdminUiVersion).setIndexPage("index.html"));
	}

	/*
	private void addMeshConfigHandler() {
		TemplateHandler javaScriptTemplateHandler = TemplateHandler.create(HandlebarsTemplateEngine.create(), "meshui-templates/config",
				"application/javascript");
		final String configFilePath = "/meshConfig.js";
		int httpPort = config().getInteger("port");
		route(configFilePath).method(GET).handler(rc -> {
			rc.put("mesh_http_port", httpPort);
			rc.next();
		});

		route(configFilePath).method(GET).handler(javaScriptTemplateHandler);
	}
	 */
	@Override
	public void registerEndPoints() throws Exception {
		//addMeshConfigHandler();
		addMeshUiStaticHandler();
		addRedirectionHandler();
	}

	@Override
	public Router setupLocalRouter() {
		Router router = Router.router(vertx);
		routerStorage.getRootRouter().mountSubRouter("/" + basePath, router);
		return router;
	}

}
