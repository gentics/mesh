package com.gentics.mesh.demo.verticle;

import static com.gentics.mesh.demo.DemoZipHelper.unzip;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializerImpl;
import com.gentics.mesh.demo.DemoDataProvider;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Demo verticle that is used to setup basic demo data.
 */
@Singleton
public class DemoVerticle extends AbstractVerticle {

	private static Logger log = LoggerFactory.getLogger(DemoVerticle.class);

	private DemoDataProvider demoDataProvider;

	@Inject
	public DemoVerticle(DemoDataProvider demoDataProvider) {
		this.demoDataProvider = demoDataProvider;
	}

	@Override
	public void start(Promise<Void> startFuture) throws Exception {
		File outputDir = new File("demo");
		if (!outputDir.exists()) {
			unzip("/mesh-demo.zip", outputDir.getAbsolutePath());
		}
		// We only want to setup the demo data once
		if (BootstrapInitializerImpl.isInitialSetup) {
			vertx.executeBlocking(bc -> {
				try {
					demoDataProvider.setup(true);
					bc.complete();
				} catch (Exception e) {
					log.error("Error while generating demo data.", e);
					bc.fail(e);
				}
			}, false, rh -> {
				if (rh.failed()) {
					startFuture.fail(rh.cause());
				} else {
					log.warn("--------------------------------");
					log.warn("- Demo setup complete          -");
					log.warn("--------------------------------");
					log.warn("- http://localhost:8080/demo   -");
					log.warn("- Login: webclient/webclient   -");
					log.warn("--------------------------------");
					startFuture.complete();
				}
			});
		} else {
			log.info("Demo graph was already setup once. Not invoking demo data setup.");
			vertx.executeBlocking(bc -> {
				demoDataProvider.invokeFullIndex();
				bc.complete();
			}, false, rh -> {
				startFuture.complete();
			});
		}

	}

}
