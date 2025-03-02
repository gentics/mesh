package com.gentics.mesh.core.endpoint.admin;

import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.MonitoringCrudHandler;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

/**
 * Endpoint definition for health / readiness checks
 */
public class HealthEndpoint extends AbstractInternalEndpoint {

	private MonitoringCrudHandler monitoringCrudHandler;

	@Inject
	public HealthEndpoint(MeshAuthChain chain, MonitoringCrudHandler monitoringCrudHandler, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super("health", chain, localConfigApi, db, options);
		this.monitoringCrudHandler = monitoringCrudHandler;
	}

	public HealthEndpoint() {
		super("health", null, null, null, null);
	}

	@Override
	public void registerEndPoints() {
		addLive();
		addReady();
		addWritable();
	}

	@Override
	public String getDescription() {
		return "Endpoints for Mesh health checks";
	}

	private void addLive() {
		InternalEndpointRoute deployEndpoint = createRoute();
		deployEndpoint.path("/live");
		deployEndpoint.method(GET);
		deployEndpoint.description("Returns an empty response with status code 200 if Gentics Mesh is alive.");
		deployEndpoint.handler(rc -> monitoringCrudHandler.handleLive(rc));
	}

	private void addReady() {
		InternalEndpointRoute deployEndpoint = createRoute();
		deployEndpoint.path("/ready");
		deployEndpoint.method(GET);
		deployEndpoint.description("Returns an empty response with status code 200 if Gentics Mesh is ready. Responds with 503 otherwise.");
		deployEndpoint.handler(rc -> monitoringCrudHandler.handleReady(rc));
	}

	private void addWritable() {
		InternalEndpointRoute deployEndpoint = createRoute();
		deployEndpoint.path("/writable");
		deployEndpoint.method(GET);
		deployEndpoint.description("Returns an empty response with status code 200 if Gentics Mesh is writable. Responds with 503 otherwise.");
		deployEndpoint.handler(rc -> monitoringCrudHandler.handleWritable(rc));
	}
}
