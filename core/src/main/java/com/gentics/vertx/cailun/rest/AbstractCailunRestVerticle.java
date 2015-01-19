package com.gentics.vertx.cailun.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.apex.core.Route;
import io.vertx.ext.apex.core.Router;

public class AbstractCailunRestVerticle extends AbstractVerticle {

	private static final Gson GSON = new GsonBuilder().create();

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

		Router rootRouter = Router.router(vertx);
		Router apiRouter = Router.router(vertx);
		rootRouter.mountSubRouter("/api/v1", apiRouter);
		Router localRouter = Router.router(vertx);
		apiRouter.mountSubRouter("/" + basePath, localRouter);

		this.localRouter = localRouter;

		HttpServer server = vertx.createHttpServer(new HttpServerOptions().setPort(8080));
		server.requestHandler(rootRouter::accept);
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
}
