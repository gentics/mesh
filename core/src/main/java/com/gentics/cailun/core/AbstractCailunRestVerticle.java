package com.gentics.cailun.core;

import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.apex.core.Route;
import io.vertx.ext.apex.core.Router;
import io.vertx.ext.apex.core.RoutingContext;

import com.gentics.cailun.auth.CaiLunAuthServiceImpl;
import com.gentics.cailun.etc.RouterStorage;
import com.gentics.cailun.etc.config.CaiLunConfigurationException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class AbstractCailunRestVerticle extends AbstractCaiLunVerticle {

	private static final Gson GSON = new GsonBuilder().create();

	// TODO use a common source
	public static final String APPLICATION_JSON = "application/json";

	protected Router localRouter = null;
	protected String basePath;

	protected AbstractCailunRestVerticle(String basePath) {
		this.basePath = basePath;
	}

	@Override
	public void start() throws Exception {

		this.localRouter = setupLocalRouter();
		if (localRouter == null) {
			throw new CaiLunConfigurationException("The local router was not setup correctly. Startup failed.");
		}
		HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8080));
		RouterStorage routerStorage = config.routerStorage();
		server.requestHandler(routerStorage.getRootRouter()::accept);
		server.listen();
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

	/**
	 * Wrapper for getRouter().route(path)
	 * 
	 * @return
	 */
	protected Route route(String path) {
		return localRouter.route(path);
	}

	/**
	 * Wrapper for getRouter().route()
	 * 
	 * @return
	 */
	protected Route route() {
		return localRouter.route();
	}

	protected String toJson(Object obj) {
		return GSON.toJson(obj);
	}

	@SuppressWarnings("unchecked")
	protected <T> T fromJson(RoutingContext rc, Class<?> classOfT) {
		return (T) GSON.fromJson(rc.getBodyAsString(), classOfT);
	}

	/**
	 * Returns the cailun auth service which can be used to authenticate resources.
	 * 
	 * @return
	 */
	protected CaiLunAuthServiceImpl getAuthService() {
		return config.authService();
	}

}
