package com.gentics.mesh.verticle.admin;

import static io.vertx.core.http.HttpMethod.GET;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class AdminGUI2Endpoint extends AbstractInternalEndpoint {

	private static final Logger log = LoggerFactory.getLogger(AdminGUI2Endpoint.class);

	private static String meshAdminUi2Version = readBuildProperties().getProperty("mesh.admin-ui2.version");

	public AdminGUI2Endpoint() {
		super("ui", null);
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
			buildProperties.load(AdminGUI2Endpoint.class.getResourceAsStream("/mesh-admin-gui.properties"));
			return buildProperties;
		} catch (Exception e) {
			log.error("Error while loading build properties", e);
			return null;
		}
	}

	private void addRedirectionHandler() {
		route().method(GET).handler(rc -> {
			if ("/ui".equals(rc.request().path())) {
				rc.response().setStatusCode(302);
				rc.response().headers().set("Location", "/" + basePath + "/");
				rc.response().end();
			} else {
				rc.next();
			}
		});
	}

	private void addMeshUi2StaticHandler() {
		StaticHandler handler = StaticHandler.create("META-INF/resources/webjars/mesh-ui/" + meshAdminUi2Version);
		handler.setDefaultContentEncoding("UTF-8");
		handler.setIndexPage("index.html");
		route("/*").method(GET).blockingHandler(handler);
	}

	@Override
	public void registerEndPoints() {
		addRedirectionHandler();
		addMeshUi2StaticHandler();
	}

}
