package com.gentics.mesh.pub;

import javax.inject.Inject;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.PublicHttpServerConfig;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.impl.RouterImpl;

public class PublicAPIVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(PublicAPIVerticle.class);

	protected HttpServer server;

	private final MeshOptions options;

	@Inject
	public PublicAPIVerticle(MeshOptions options) {
		this.options = options;
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		PublicHttpServerConfig config = options.getPublicHttpServerOptions();

		int port = config.getPort();
		String host = config.getHost();
		HttpServerOptions options = new HttpServerOptions();
		options.setPort(port);
		options.setHost(host);
		options.setCompressionSupported(true);
		options.setHandle100ContinueAutomatically(true);
		Router router = new RouterImpl(vertx);
		router.route("/metrics").blockingHandler(bc -> {
			bc.response().end("bla");
		});

		log.info("Starting public http server in verticle {" + getClass().getName() + "} on port {" + options.getPort() + "}");
		server = vertx.createHttpServer(options);
		server.requestHandler(router::accept);
		server.listen(rh -> {
			if (rh.failed()) {
				startFuture.fail(rh.cause());
			} else {
				if (log.isInfoEnabled()) {
					log.info("Started public http server.. Port: " + config().getInteger("port"));
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
