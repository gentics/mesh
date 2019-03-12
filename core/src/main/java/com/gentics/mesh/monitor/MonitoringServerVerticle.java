package com.gentics.mesh.monitor;

import javax.inject.Inject;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.impl.RouterImpl;

public class MonitoringServerVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(MonitoringServerVerticle.class);

	protected HttpServer server;

	private final MeshOptions options;
	
	private MonitoringRoutes routes;

	@Inject
	public MonitoringServerVerticle(MeshOptions options, MonitoringRoutes routes) {
		this.options = options;
		this.routes = routes;
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		MonitoringConfig config = options.getMonitoringOptions();

		int port = config.getPort();
		String host = config.getHost();
		HttpServerOptions options = new HttpServerOptions();
		options.setPort(port);
		options.setHost(host);
		options.setCompressionSupported(true);
		options.setHandle100ContinueAutomatically(true);
		Router router = routes.getRouter();

		log.info("Starting monitoring http server in verticle {" + getClass().getName() + "} on port {" + options.getPort() + "}");
		server = vertx.createHttpServer(options);
		server.requestHandler(router::accept);
		server.listen(rh -> {
			if (rh.failed()) {
				startFuture.fail(rh.cause());
			} else {
				if (log.isInfoEnabled()) {
					log.info("Started monitoring http server.. Port: " + config().getInteger("port"));
				}
				try {
					// registerEndPoints(storage);
					startFuture.complete();
				} catch (Exception e) {
					e.printStackTrace();
					startFuture.fail(e);
				}
			}
		});
	}

}
