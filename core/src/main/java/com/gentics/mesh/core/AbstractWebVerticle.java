package com.gentics.mesh.core;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshConfigurationException;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public abstract class AbstractWebVerticle extends AbstractSpringVerticle {

	private static final Logger log = LoggerFactory.getLogger(AbstractWebVerticle.class);

	protected Router localRouter = null;
	protected String basePath;
	protected HttpServer server;

	protected AbstractWebVerticle(String basePath) {
		this.basePath = basePath;
	}

	@Override
	public void start() throws Exception {
		this.localRouter = setupLocalRouter();
		if (localRouter == null) {
			throw new MeshConfigurationException("The local router was not setup correctly. Startup failed.");
		}
		if (log.isInfoEnabled()) {
			log.info("Starting http server..");
		}
		HttpServerOptions options = new HttpServerOptions();
		options.setPort(config().getInteger("port"));

		MeshOptions meshOptions = Mesh.mesh().getOptions();
		HttpServerConfig httpServerOptions = meshOptions.getHttpServerOptions();
		if (httpServerOptions.isSsl()) {
			if (log.isErrorEnabled()) {
				log.debug("Setting ssl server options");
			}
			options.setSsl(true);
			PemKeyCertOptions keyOptions = new PemKeyCertOptions();
			if (isEmpty(httpServerOptions.getCertPath()) || isEmpty(httpServerOptions.getKeyPath())) {
				throw new MeshConfigurationException("SSL is enabled but either the server key or the cert path was not specified.");
			}
			keyOptions.setKeyPath(httpServerOptions.getKeyPath());
			keyOptions.setCertPath(httpServerOptions.getCertPath());
			options.setPemKeyCertOptions(keyOptions);
		}

		log.info("Starting http server in verticle {" + getClass().getName() + "} on port {" + options.getPort() + "}" );
		server = vertx.createHttpServer(options);
		server.requestHandler(routerStorage.getRootRouter()::accept);
		server.listen();
		if (log.isInfoEnabled()) {
			log.info("Started http server.. Port: " + config().getInteger("port"));
		}
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
	 */
	protected Route route(String path) {
		Route route = localRouter.route(path);
		return route;
	}

	/**
	 * Wrapper for getRouter().route()
	 */
	protected Route route() {
		Route route = localRouter.route();
		return route;
	}

}
