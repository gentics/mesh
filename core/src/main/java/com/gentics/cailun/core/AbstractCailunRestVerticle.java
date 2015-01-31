package com.gentics.cailun.core;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.apex.core.Route;
import io.vertx.ext.apex.core.Router;
import io.vertx.ext.apex.core.RoutingContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.auth.CaiLunAuthServiceImpl;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AbstractCailunRestVerticle extends AbstractVerticle {

	private static final Gson GSON = new GsonBuilder().create();

	// TODO use a common source
	public static final String APPLICATION_JSON = "application/json";

	@Autowired
	CaiLunSpringConfiguration config;

	private Router localRouter = null;
	private String basePath = null;

	protected AbstractCailunRestVerticle(String basePath) {
		this.basePath = basePath;
	}

	@Override
	public void init(Vertx vertx, Context context) {
		super.init(vertx, context);
	}

	@Override
	public void start() throws Exception {
		RouterStorage routerStorage = config.routerStorage();
		this.localRouter = routerStorage.getRouter("/" + basePath);

		HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8080));
		server.requestHandler(routerStorage.getRootRouter()::accept);
		server.listen();

	}

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
