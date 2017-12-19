package com.gentics.mesh.core;

import javax.inject.Inject;
import javax.inject.Provider;

import com.gentics.mesh.auth.MeshAuthHandler;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.etc.RouterStorage;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

/**
 * A web verticle is an verticle which starts an http server and provides various routes. The started web verticle http server will use the
 * {@link RouterStorage} root router in order to setup the request handler.
 */
public abstract class AbstractWebVerticle extends AbstractVerticle {

	private static final Logger log = LoggerFactory.getLogger(AbstractWebVerticle.class);

	protected Router localRouter = null;
	protected String basePath;
	protected HttpServer server;

	@Inject
	public MeshAuthHandler authHandler;

	public RouterStorage routerStorage;

	protected AbstractWebVerticle(String basePath, Provider<RouterStorage> routerStorageProvider) {
		this.basePath = basePath;
		this.routerStorage = routerStorageProvider.get();
	}

	@Override
	public void start() throws Exception {
		start(Future.future());
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		this.localRouter = setupLocalRouter();
		if (localRouter == null) {
			throw new MeshConfigurationException("The local router was not setup correctly. Startup failed.");
		}
		int port = config().getInteger("port");
		if (log.isInfoEnabled()) {
			log.info("Starting http server on port {" + port + "}..");
		}
		HttpServerOptions options = new HttpServerOptions();
		options.setPort(port);
		options.setCompressionSupported(true);
		// MeshOptions meshOptions = Mesh.mesh().getOptions();
		// HttpServerConfig httpServerOptions = meshOptions.getHttpServerOptions();
		// if (httpServerOptions.isSsl()) {
		// if (log.isErrorEnabled()) {
		// log.debug("Setting ssl server options");
		// }
		// options.setSsl(true);
		// PemKeyCertOptions keyOptions = new PemKeyCertOptions();
		// if (isEmpty(httpServerOptions.getCertPath()) || isEmpty(httpServerOptions.getKeyPath())) {
		// throw new MeshConfigurationException("SSL is enabled but either the server key or the cert path was not specified.");
		// }
		// keyOptions.setKeyPath(httpServerOptions.getKeyPath());
		// keyOptions.setCertPath(httpServerOptions.getCertPath());
		// options.setPemKeyCertOptions(keyOptions);
		// }

		log.info("Starting http server in verticle {" + getClass().getName() + "} on port {" + options.getPort() + "}");
		server = vertx.createHttpServer(options);
		server.requestHandler(routerStorage.getRootRouter()::accept);
		server.listen(rh -> {
			if (rh.failed()) {
				startFuture.fail(rh.cause());
			} else {
				if (log.isInfoEnabled()) {
					log.info("Started http server.. Port: " + config().getInteger("port"));
				}
				try {
					registerEndPoints();
					startFuture.complete();
				} catch (Exception e) {
					startFuture.fail(e);
					return;
				}
			}
		});

	}

	/**
	 * Add a route which will secure all endpoints.
	 */
	protected void secureAll() {
		getRouter().route("/*").handler(authHandler);
	}

	/**
	 * Register all endpoints to the local router.
	 * 
	 * @throws Exception
	 */
	public abstract void registerEndPoints() throws Exception;

	/**
	 * Setup the api sub router for the basepath of the verticle.
	 * 
	 * @return
	 */
	public Router setupLocalRouter() {
		return routerStorage.getAPISubRouter(basePath);
	}

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
	 * @param path
	 * @return
	 */
	protected Route route(String path) {
		Route route = getRouter().route(path);
		return route;
	}

	/**
	 * Wrapper for getRouter().route()
	 */
	protected Route route() {
		Route route = getRouter().route();
		return route;
	}

	public String getBasePath() {
		return basePath;
	}
}
