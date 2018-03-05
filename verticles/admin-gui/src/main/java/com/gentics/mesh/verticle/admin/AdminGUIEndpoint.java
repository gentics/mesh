package com.gentics.mesh.verticle.admin;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.vertx.core.http.HttpMethod.GET;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.MapValueResolver;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class AdminGUIEndpoint extends AbstractInternalEndpoint {

	private static final Logger log = LoggerFactory.getLogger(AdminGUIEndpoint.class);

	public static final String CONF_FILE = "mesh-ui-config.js";

	// TODO handle NPEs
	private static String meshAdminUiVersion = readBuildProperties().getProperty("mesh.admin-ui.version");

	public AdminGUIEndpoint() {
		super("mesh-ui");
	}

	@Override
	public String getDescription() {
		return "Endpoint which provides the mesh-ui webapp";
	}

	@Override
	public void init(RouterStorage rs) {
		Router router = Router.router(Mesh.vertx());
		rs.root().getRouter().mountSubRouter("/" + basePath, router);
		this.routerStorage = rs;
		this.localRouter = router;
	}

	private static Properties readBuildProperties() {
		try {
			Properties buildProperties = new Properties();
			buildProperties.load(AdminGUIEndpoint.class.getResourceAsStream("/mesh-admin-gui.properties"));
			return buildProperties;
		} catch (Exception e) {
			log.error("Error while loading build properties", e);
			return null;
		}
	}

	private void addRedirectionHandler() {
		routerStorage.root().getRouter().route("/").method(GET).handler(rc -> {
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
		StaticHandler handler = StaticHandler.create("META-INF/resources/webjars/mesh-ui/" + meshAdminUiVersion);
		handler.setIndexPage("index.html");
		route("/*").method(GET).blockingHandler(handler);
	}

	private void addMeshConfigHandler() {
		route("/" + CONF_FILE).method(GET).handler(rc -> {
			rc.response().putHeader("Content-Type", "application/javascript");
			rc.response().sendFile(CONFIG_FOLDERNAME + "/" + CONF_FILE);
		});
	}

	@Override
	public void registerEndPoints() {
		addMeshConfigHandler();
		addRedirectionHandler();
		saveMeshUiConfig();
		addMeshUiStaticHandler();
	}

	private void saveMeshUiConfig() {
		File parentFolder = new File(CONFIG_FOLDERNAME);
		if (!parentFolder.exists() && !parentFolder.mkdirs()) {
			throw error(INTERNAL_SERVER_ERROR, "Could not create configuration folder {" + parentFolder.getAbsolutePath() + "}");
		}
		File outputFile = new File(parentFolder, CONF_FILE);
		if (!outputFile.exists()) {
			InputStream ins = getClass().getResourceAsStream("/meshui-templates/mesh-ui-config.hbs");
			if (ins == null) {
				throw error(INTERNAL_SERVER_ERROR, "Could not find mesh-ui config template within classpath.");
			}
			try {
				Handlebars handlebars = new Handlebars();
				Template template = handlebars.compileInline(IOUtils.toString(ins));

				Map<String, Object> model = new HashMap<>();
				int httpPort = Mesh.mesh().getOptions().getHttpServerOptions().getPort();
				model.put("mesh_http_port", httpPort);

				// Prepare render context
				Context context = Context.newBuilder(model).resolver(MapValueResolver.INSTANCE).build();
				FileWriter writer = new FileWriter(outputFile);
				template.apply(context, writer);
				writer.close();
			} catch (Exception e) {
				log.error("Could not save configuration file {" + outputFile.getAbsolutePath() + "}");
			}
		}
	}

}
