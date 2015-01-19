package com.gentics.vertx.cailun.rest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.apex.core.Route;
import io.vertx.ext.apex.core.Router;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AbstractCailunRestVerticle extends AbstractVerticle {

	private static final Gson GSON = new GsonBuilder().create();

	public static final String APPLICATION_JSON = "application/json";

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
		RouterStorage routerStorage = RouterStorage.getInstance(vertx);
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

	protected <T> T fromJson(HttpServerRequest request, Class<T> classOf) {
		return null;
		// return GSON.fromJson(request., classOfT)
	}

}
