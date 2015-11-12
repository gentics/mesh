package com.gentics.mesh.demo.verticle;

import static com.gentics.mesh.demo.DemoZipHelper.unzip;
import static io.vertx.core.http.HttpMethod.GET;

import java.io.File;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.AbstractCustomVerticle;
import com.gentics.mesh.demo.DemoDataProvider;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * Demo verticle that is used to setup basic demo data.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class DemoVerticle extends AbstractCustomVerticle {

	private static Logger log = LoggerFactory.getLogger(DemoVerticle.class);

	@Autowired
	private DemoDataProvider demoDataProvider;

	public DemoVerticle() {
		super("demo-verticle");
	}

	@Override
	public void registerEndPoints() throws Exception {
		// We only want to setup the demo data once
		if (BootstrapInitializer.isInitialSetup) {
			demoDataProvider.setup();
		} else {
			log.info("Demo graph was already setup once. Not invoking demo data setup.");
		}

		File outputDir = new File("demo");
		if (!outputDir.exists()) {
			unzip("/mesh-demo.zip", outputDir.getAbsolutePath());
		}
		StaticHandler staticHandler = StaticHandler.create("demo");
		staticHandler.setDirectoryListing(false);
		staticHandler.setCachingEnabled(false);
		staticHandler.setIndexPage("index.html");
		routerStorage.getRootRouter().route("/demo").handler(rc -> {
			rc.response().setStatusCode(302);
			rc.response().headers().set("Location", "/demo/index.html");
			rc.response().end();
		});
		routerStorage.getRootRouter().route("/demo/*").method(GET).handler(staticHandler);

	}

}
