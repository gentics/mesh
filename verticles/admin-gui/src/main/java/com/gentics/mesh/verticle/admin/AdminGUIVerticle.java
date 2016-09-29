package com.gentics.mesh.verticle.admin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.vertx.core.http.HttpMethod.GET;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.etc.RouterStorage;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.MapValueResolver;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;


@Singleton
public class AdminGUIVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(AdminGUIVerticle.class);

	public static final String CONF_FILE = "mesh-ui-config.js";

	// TODO handle NPEs
	private static String meshAdminUiVersion = readBuildProperties().getProperty("mesh.admin-ui.version");

	@Inject
	public AdminGUIVerticle(RouterStorage routerStorage) {
		super("mesh-ui", routerStorage);
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which provides the mesh admin ui";
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
		route().method(GET).handler(rc -> {
			if ("/mesh-ui".equals(rc.request().path())) {
				rc.response().setStatusCode(302);
				rc.response().headers().set("Location", "/" + basePath + "/");
				rc.response().end();
			} else {
				rc.next();
			}
		});
	}

	private void addMeshUiStaticHandler() {
		route("/*").method(GET).handler(StaticHandler.create("META-INF/resources/webjars/mesh-ui/" + meshAdminUiVersion).setIndexPage("index.html"));
	}

	private void addMeshConfigHandler() {
		route("/" + CONF_FILE).method(GET).handler(rc -> {
			rc.response().putHeader("Content-Type", "application/javascript");
			rc.response().sendFile(CONF_FILE);
		});
	}

	@Override
	public void registerEndPoints() throws Exception {
		addMeshConfigHandler();
		addRedirectionHandler();
		saveMeshUiConfig();
		addMeshUiStaticHandler();
	}

	private void saveMeshUiConfig() {
		File outputFile = new File(CONF_FILE);
		if (!outputFile.exists()) {
			InputStream ins = getClass().getResourceAsStream("/meshui-templates/mesh-ui-config.hbs");
			if (ins == null) {
				throw error(INTERNAL_SERVER_ERROR, "Could not find mesh ui config template");
			}
			try {
				Handlebars handlebars = new Handlebars();
				Template template = handlebars.compileInline(IOUtils.toString(ins));

				Map<String, Object> model = new HashMap<>();
				int httpPort = config().getInteger("port");
				model.put("mesh_http_port", httpPort);

				// Prepare render context
				Context context = Context.newBuilder(model).resolver(MapValueResolver.INSTANCE).build();
				FileWriter writer = new FileWriter(outputFile);
				template.apply(context, writer);
				writer.close();
			} catch (Exception e) {
				log.error("Could not save configuration file {" + CONF_FILE + "}");
			}
		}
	}

	@Override
	public Router setupLocalRouter() {
		Router router = Router.router(vertx);
		routerStorage.getRootRouter().mountSubRouter("/" + basePath, router);
		return router;
	}

}
