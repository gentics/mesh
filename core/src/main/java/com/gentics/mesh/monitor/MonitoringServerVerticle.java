package com.gentics.mesh.monitor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

/**
 * Verticle which starts the monitoring server (default port 8081).
 */
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
	public void start(Promise<Void> promise) throws Exception {
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
		server.requestHandler(router);
		server.listen().onComplete(res -> {
			if (log.isInfoEnabled()) {
				log.info("Started monitoring http server. Port: " + options.getPort());
			}
		}, err -> {
			promise.fail(err);
		});
	}

	@Override
	public void stop(Promise<Void> promise) throws Exception {
		server.close().onComplete(res -> {
			if (log.isInfoEnabled()) {
				log.info("Monitoring http server stopped.");
			}
		}, err -> {
			promise.fail(err);
		});
	}

}
