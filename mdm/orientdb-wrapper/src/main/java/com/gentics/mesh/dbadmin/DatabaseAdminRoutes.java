package com.gentics.mesh.dbadmin;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.core.endpoint.handler.DatabaseAdminHandler;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.monitor.liveness.LivenessManager;
import com.gentics.mesh.router.route.DefaultNotFoundHandler;
import com.gentics.mesh.router.route.FailureHandler;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.impl.RouterImpl;

public class DatabaseAdminRoutes {

	private final RouterImpl router;
	private final RouterImpl apiRouter;
	private final DatabaseAdminHandler databaseAdminHandler;
	private final OrientDBMeshOptions options;
	private final LivenessManager liveness;
	private final MeshAuthChainImpl chain;

	@Inject
	public DatabaseAdminRoutes(Vertx vertx, OrientDBMeshOptions options, DatabaseAdminHandler databaseAdminHandler, LivenessManager liveness, MeshAuthChainImpl chain) {
		this.router = new RouterImpl(vertx);
		this.apiRouter = new RouterImpl(vertx);
		this.databaseAdminHandler = databaseAdminHandler;
		this.options = options;
		this.liveness = liveness;
		this.chain = chain;
		VersionUtils.generateVersionMountpoints().forEach(mountPoint -> router.mountSubRouter(mountPoint, apiRouter));
		init();
	}

	/**
	 * Initialize the monitoring routes.
	 */
	public void init() {
		router.route().handler(LoggerHandler.create());
		router.route().last().handler(DefaultNotFoundHandler.create());
		router.route().failureHandler(FailureHandler.create(liveness));

		addStartStop();
	}

	/**
	 * Reacting to the database start/stop requests
	 */
	private void addStartStop() {
		maybeSecure(
			apiRouter.route("/dbstop")
				.method(HttpMethod.POST)
				.produces(APPLICATION_JSON)
				.handler(wrapClientLimit(rc -> {
					databaseAdminHandler.handleDatabaseStop(rc);
				})));
		maybeSecure(
			apiRouter.route("/dbstart")
				.method(HttpMethod.POST)
				.produces(APPLICATION_JSON)
				.handler(rc -> {
					databaseAdminHandler.handleDatabaseStart(rc);
				}));
	}

	public Router getRouter() {
		return router;
	}

	private Handler<RoutingContext> wrapClientLimit(Handler<RoutingContext> inner) {
		return rc -> {
			if (options.getStorageOptions().getAdministrationOptions() != null 
					&& options.getStorageOptions().getAdministrationOptions().isLocalOnly()
					&& !isLocalClient(rc)) {
				rc.fail(FORBIDDEN.code());
			} else {
				inner.handle(rc);
			}			
		};
	}

	private boolean isLocalClient(RoutingContext rc) {
		return rc.request().host().equals(rc.request().remoteAddress().host()) || "127.0.0.1".equals(rc.request().host())  || "localhost".equals(rc.request().host());
	}

	private void maybeSecure(Route route) {
		if (options.getStorageOptions().getAdministrationOptions() != null && !options.getStorageOptions().getAdministrationOptions().isLocalOnly()) {
			chain.secure(route);
		}
	}
}
