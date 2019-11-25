package com.gentics.mesh.core.endpoint.admin;

import static io.vertx.core.http.HttpMethod.GET;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.core.endpoint.handler.MonitoringCrudHandler;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

public class HealthEndpoint extends AbstractInternalEndpoint {
	private final MonitoringCrudHandler monitoringCrudHandler;

	@Inject
	public HealthEndpoint(MeshAuthChain chain, MonitoringCrudHandler monitoringCrudHandler) {
		super("health", chain);
		this.monitoringCrudHandler = monitoringCrudHandler;
	}

	@Override
	public void registerEndPoints() {
		addLive();
		addReady();
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
		deployEndpoint.handler(monitoringCrudHandler::handleLive);
	}

	private void addReady() {
		InternalEndpointRoute deployEndpoint = createRoute();
		deployEndpoint.path("/ready");
		deployEndpoint.method(GET);
		deployEndpoint.description("Returns an empty response with status code 200 if Gentics Mesh is ready. Responds with 503 otherwise.");
		deployEndpoint.handler(monitoringCrudHandler::handleReady);
	}
}
