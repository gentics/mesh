package com.gentics.mesh.demo.verticle;

import static com.gentics.mesh.demo.DemoZipHelper.unzip;
import static io.vertx.core.http.HttpMethod.GET;

import java.io.File;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * Demo verticle that is used to setup basic demo data.
 */
public class DemoVerticle extends AbstractWebVerticle {

	private static Logger log = LoggerFactory.getLogger(DemoVerticle.class);

	private DemoDataProvider demoDataProvider;

	@Inject
	public DemoVerticle(DemoDataProvider demoDataProvider, RouterStorage routerStorage, MeshSpringConfiguration springConfig) {
		super("demo", routerStorage, springConfig);
		this.demoDataProvider = demoDataProvider;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which serve the demo application";
	}

	private void addRedirectionHandler() {
		route().method(GET).handler(rc -> {
			if ("/demo".equals(rc.request().path())) {
				rc.response().setStatusCode(302);
				rc.response().headers().set("Location", "/" + basePath + "/");
				rc.response().end();
			} else {
				rc.next();
			}
		});
	}

	@Override
	public void registerEndPoints() throws Exception {
		// We only want to setup the demo data once
		if (BootstrapInitializer.isInitialSetup) {
			vertx.executeBlocking(bc -> {
				try {
					demoDataProvider.setup();
				} catch (Exception e) {
					log.error("Error while generating demo data.", e);
				}
			}, rh -> {
				System.out.println("Done");
			});
		} else {
			log.info("Demo graph was already setup once. Not invoking demo data setup.");
		}

		File outputDir = new File("demo");
		if (!outputDir.exists()) {
			unzip("/mesh-demo.zip", outputDir.getAbsolutePath());
		}

		addRedirectionHandler();
		StaticHandler staticHandler = StaticHandler.create("demo");
		staticHandler.setDirectoryListing(false);
		staticHandler.setCachingEnabled(false);
		staticHandler.setIndexPage("index.html");

		route("/*").method(GET).handler(staticHandler);

		log.warn("--------------------------------");
		log.warn("- Demo setup complete          -");
		log.warn("--------------------------------");
		log.warn("- http://localhost:8080/demo   -");
		log.warn("- Login: webclient/webclient   -");
		log.warn("--------------------------------");
	}

	@Override
	public Router setupLocalRouter() {
		Router router = Router.router(vertx);
		routerStorage.getRootRouter().mountSubRouter("/" + basePath, router);
		return router;
	}

}
