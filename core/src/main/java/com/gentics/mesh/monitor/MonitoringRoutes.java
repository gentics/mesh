package com.gentics.mesh.monitor;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.core.endpoint.admin.HealthEndpoint;
import com.gentics.mesh.core.endpoint.handler.MonitoringCrudHandler;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.monitor.liveness.LivenessManager;
import com.gentics.mesh.router.route.DefaultNotFoundHandler;
import com.gentics.mesh.router.route.FailureHandler;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.impl.RouterImpl;
import io.vertx.micrometer.PrometheusScrapingHandler;

/**
 * Routes for the monitoring server. Not to be confused with {@link HealthEndpoint} which is used for the Mesh REST server. 
 */
public class MonitoringRoutes {

	private static final Logger log = LoggerFactory.getLogger(MonitoringRoutes.class);

	private final BootstrapInitializer boot;

	private final RouterImpl router;

	private final RouterImpl apiRouter;

	private final AdminHandler adminHandler;

	private final MeshOptions options;

	private final MonitoringCrudHandler monitoringCrudHandler;

	private final LivenessManager liveness;

	@Inject
	public MonitoringRoutes(Vertx vertx, BootstrapInitializer boot, AdminHandler adminHandler, MeshOptions options, MonitoringCrudHandler monitoringCrudHandler, LivenessManager liveness) {
		this.router = new RouterImpl(vertx);
		this.boot = boot;
		this.apiRouter = new RouterImpl(vertx);
		this.options = options;
		this.monitoringCrudHandler = monitoringCrudHandler;
		this.liveness = liveness;
		VersionUtils.generateVersionMountpoints()
			.forEach(mountPoint -> router.mountSubRouter(mountPoint, apiRouter));
		this.adminHandler = adminHandler;
		init();
	}

	/**
	 * Initialize the monitoring routes.
	 */
	public void init() {
		router.route().handler(LoggerHandler.create());
		router.route().last().handler(DefaultNotFoundHandler.create(options.getHttpServerOptions()));
		router.route().failureHandler(FailureHandler.create(liveness, options.getHttpServerOptions()));

		addMetrics();
		addLive();
		addReady();
		addWritable();
		addVersion();
		addStatus();
		addClusterStatus();
	}

	/**
	 * Handler that reacts onto status requests.
	 */
	private void addStatus() {
		// endpoint.description("Return the Gentics Mesh server status.");
		// endpoint.exampleResponse(OK, adminExamples.createMeshStatusResponse(MeshStatus.READY), "Status of the Gentics Mesh server.");
		apiRouter.route("/status")
			.method(GET)
			.produces(APPLICATION_JSON)
			.handler(rc -> {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				adminHandler.handleMeshStatus(ac);
			});
	}

	private void addClusterStatus() {
		// endpoint.description("Loads the cluster status information.");
		// endpoint.exampleResponse(OK, adminExamples.createClusterStatusResponse(), "Cluster status.");
		apiRouter.route("/cluster/status")
			.method(GET)
			.produces(APPLICATION_JSON)
			.handler(rc -> {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				adminHandler.handleClusterStatus(ac);
			});
	}

	private void addLive() {
		apiRouter.route("/health/live")
			.method(GET)
			.handler(monitoringCrudHandler::handleLive);
	}

	private void addReady() {
		apiRouter.route("/health/ready")
			.method(GET)
			.handler(monitoringCrudHandler::handleReady);
	}

	private void addWritable() {
		apiRouter.route("/health/writable")
			.method(GET)
			.handler(monitoringCrudHandler::handleWritable);
	}

	private void addMetrics() {
		// metrics.description("Returns the stored system metrics.");
		apiRouter.route("/metrics")
			.handler(PrometheusScrapingHandler.create(options.getNodeName()));
	}

	private void addVersion() {
		// infoEndpoint.description("Endpoint which returns version information");
		// infoEndpoint.displayName("Version Information");
		// infoEndpoint.exampleResponse(OK, examples.getInfoExample(), "JSON which contains version information");
		apiRouter.route("/versions")
			.produces(APPLICATION_JSON)
			.method(GET)
			.handler(rc -> {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				adminHandler.handleVersions(ac);
			});
	}

	public Router getRouter() {
		return router;
	}
}
