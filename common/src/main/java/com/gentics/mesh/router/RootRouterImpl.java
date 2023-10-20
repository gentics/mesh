package com.gentics.mesh.router;

import static com.gentics.mesh.handler.VersionHandlerImpl.API_MOUNTPOINT;

import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.monitor.liveness.LivenessManager;
import com.gentics.mesh.router.route.DefaultNotFoundHandler;
import com.gentics.mesh.router.route.FailureHandler;
import com.gentics.mesh.router.route.PoweredByHandler;
import com.gentics.mesh.router.route.SecurityLoggingHandler;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;

/**
 * @see RootRouter
 */
public class RootRouterImpl implements RootRouter {

	private final APIRouter apiRouter;

	private final CustomRouter customRouter;

	private final Router router;

	private RouterStorage storage;

	private Vertx vertx;

	public RootRouterImpl(Vertx vertx, RouterStorage storage, MeshOptions options, LivenessManager livenessBean) {
		this.storage = storage;
		this.vertx = vertx;
		this.router = Router.router(vertx);
		// Root handlersA
		router.route().handler(LoggerHandler.create(LoggerFormat.SHORT));
		// TODO add a dedicated error for api router that informs about
		// APPLICATION_JSON requirements. This may not be true for other
		// routes (eg. custom
		// routes)
		router.route().last().handler(DefaultNotFoundHandler.create(options.getHttpServerOptions()));
		router.route().failureHandler(FailureHandler.create(livenessBean, options.getHttpServerOptions()));
		if (options.getHttpServerOptions().isServerTokens()) {
			router.route().handler(PoweredByHandler.create());
		}
		router.route().handler(SecurityLoggingHandler.create());
		router.route(API_MOUNTPOINT).handler(storage.getVersionHandler());

		this.apiRouter = new APIRouterImpl(vertx, this, options);
		this.customRouter = new CustomRouterImpl(vertx, this);
	}

	@Override
	public Router getRouter() {
		return router;
	}

	@Override
	public APIRouter apiRouter() {
		return apiRouter;
	}

	@Getter
	public CustomRouter customRouter() {
		return customRouter;
	}

	public Vertx getVertx() {
		return vertx;
	}

	@Override
	public RouterStorage getStorage() {
		return storage;
	}

}
