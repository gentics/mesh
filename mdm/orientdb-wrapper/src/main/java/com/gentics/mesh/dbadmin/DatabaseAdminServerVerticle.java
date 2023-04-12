package com.gentics.mesh.dbadmin;

import javax.inject.Inject;

import com.gentics.mesh.etc.config.AdministrationOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;

public class DatabaseAdminServerVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(DatabaseAdminServerVerticle.class);

	protected HttpServer server;

	private final OrientDBMeshOptions options;

	private DatabaseAdminRoutes routes;

	@Inject
	public DatabaseAdminServerVerticle(OrientDBMeshOptions options, DatabaseAdminRoutes routes) {
		this.options = options;
		this.routes = routes;
	}

	@Override
	public void start(Promise<Void> promise) throws Exception {
		AdministrationOptions config = options.getStorageOptions().getAdministrationOptions();
		int port = config.getPort();
		String host = config.getHost();
		HttpServerOptions options = new HttpServerOptions();
		options.setPort(port);
		options.setHost(host);
		options.setCompressionSupported(true);
		options.setHandle100ContinueAutomatically(true);
		Router router = routes.getRouter();

		log.info("Starting DB admin http server in verticle {" + getClass().getName() + "} on port {" + options.getPort() + "}");
		server = vertx.createHttpServer(options);
		server.requestHandler(router);
		server.listen(rh -> {
			if (rh.failed()) {
				promise.fail(rh.cause());
			} else {
				if (log.isInfoEnabled()) {
					log.info("Started DB admin http server.. Port: " + options.getPort());
				}
				promise.complete();
			}
		});
	}

	@Override
	public void stop(Promise<Void> promise) throws Exception {
		server.close(rh -> {
			if (rh.failed()) {
				promise.fail(rh.cause());
			} else {
				promise.complete();
			}
		});
	}
}
