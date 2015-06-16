package com.gentics.mesh.core;

import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.etc.config.MeshConfigurationException;

public abstract class AbstractRestVerticle extends AbstractSpringVerticle {

	private static final Logger log = LoggerFactory.getLogger(AbstractRestVerticle.class);

	public static final String APPLICATION_JSON = ContentType.APPLICATION_JSON.getMimeType();

	protected Router localRouter = null;
	protected String basePath;
	protected HttpServer server;

	protected AbstractRestVerticle(String basePath) {
		this.basePath = basePath;
	}

	@Override
	public void start() throws Exception {
		this.localRouter = setupLocalRouter();
		if (localRouter == null) {
			throw new MeshConfigurationException("The local router was not setup correctly. Startup failed.");
		}

		log.info("Starting http server..");
		server = vertx.createHttpServer(new HttpServerOptions().setPort(config().getInteger("port")));
		server.requestHandler(routerStorage.getRootRouter()::accept);
		server.listen();
		log.info("Started http server.. Port: " + config().getInteger("port"));
		registerEndPoints();

	}

	public abstract void registerEndPoints() throws Exception;

	public abstract Router setupLocalRouter();

	@Override
	public void stop() throws Exception {
		localRouter.clear();
	}

	public Router getRouter() {
		return localRouter;
	}

	public HttpServer getServer() {
		return server;
	}

	/**
	 * Wrapper for getRouter().route(path)
	 * 
	 * @return
	 */
	protected Route route(String path) {
		Route route = localRouter.route(path);
		return route;
	}

	/**
	 * Wrapper for getRouter().route()
	 * 
	 * @return
	 */
	protected Route route() {
		Route route = localRouter.route();
		return route;
	}

}
