package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoHandler;
import com.gentics.mesh.core.endpoint.admin.plugin.PluginHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.rest.InternalEndpointRoute;

public class ClusterAdminEndpointImpl extends AdminEndpointImpl {
	
	protected final OrientDBAdminHandler adminHandler;

	@Inject
	public ClusterAdminEndpointImpl(MeshAuthChainImpl chain, OrientDBAdminHandler adminHandler, JobHandler jobHandler,
			ConsistencyCheckHandler consistencyHandler, PluginHandler pluginHandler, DebugInfoHandler debugInfoHandler,
			LocalConfigHandler localConfigHandler, ShutdownHandler shutdownHandler, HandlerUtilities handlerUtilities) {
		super(chain, adminHandler, jobHandler, consistencyHandler, pluginHandler, debugInfoHandler, localConfigHandler,
				shutdownHandler, handlerUtilities);
		this.adminHandler = adminHandler;
	}
	
	public void registerEndPoints() {
		super.registerEndPoints();
		addClusterConfigHandler();
		addCoordinatorHandler();
	}

	private void addClusterConfigHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/cluster/config");
		endpoint.method(GET);
		endpoint.description("Loads the cluster configuration.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, adminExamples.createClusterConfigResponse(), "Currently active cluster configuration.");
		endpoint.blockingHandler(rc -> {
			adminHandler.handleLoadClusterConfig(wrap(rc));
		});

		InternalEndpointRoute updateEndpoint = createRoute();
		updateEndpoint.path("/cluster/config");
		updateEndpoint.method(POST);
		updateEndpoint.description("Update the cluster configuration.");
		updateEndpoint.produces(APPLICATION_JSON);
		updateEndpoint.exampleRequest(adminExamples.createClusterConfigRequest());
		updateEndpoint.exampleResponse(OK, adminExamples.createClusterConfigResponse(), "Updated cluster configuration.");
		updateEndpoint.blockingHandler(rc -> {
			adminHandler.handleUpdateClusterConfig(wrap(rc));
		});
	}	

	private void addCoordinatorHandler() {
		InternalEndpointRoute loadMaster = createRoute();
		loadMaster.path("/coordinator/master");
		loadMaster.method(GET);
		loadMaster.produces(APPLICATION_JSON);
		loadMaster.description("Returns information on the elected coordinator master.");
		loadMaster.exampleResponse(OK, adminExamples.createCoordinatorResponse(), "Currently elected master.");
		loadMaster.handler(rc -> adminHandler.handleLoadCoordinationMaster(wrap(rc)));

		InternalEndpointRoute electMaster = createRoute();
		electMaster.path("/coordinator/master");
		electMaster.method(POST);
		electMaster.produces(APPLICATION_JSON);
		electMaster.description("Make this instance the coordination master.");
		electMaster.exampleResponse(OK, miscExamples.createMessageResponse(), "Election status message.");
		electMaster.handler(rc -> adminHandler.handleSetCoordinationMaster(wrap(rc)));

		InternalEndpointRoute loadConfig = createRoute();
		loadConfig.path("/coordinator/config");
		loadConfig.method(GET);
		loadConfig.produces(APPLICATION_JSON);
		loadConfig.description("Returns the currently active coordination configuration.");
		loadConfig.exampleResponse(OK, adminExamples.createCoordinatorConfig(), "The currently active coordination config on this instance.");
		loadConfig.handler(rc -> adminHandler.handleLoadCoordinationConfig(wrap(rc)));

		InternalEndpointRoute updateConfig = createRoute();
		updateConfig.path("/coordinator/config");
		updateConfig.method(POST);
		updateConfig.produces(APPLICATION_JSON);
		updateConfig.description("Update the coordinator configuration of this instance. Note that the updated config will not be persisted.");
		updateConfig.exampleResponse(OK, adminExamples.createCoordinatorConfig(), "The currently active config on this instance.");
		updateConfig.exampleRequest(adminExamples.createCoordinatorConfigRequest());
		updateConfig.handler(rc -> adminHandler.handleUpdateCoordinationConfig(wrap(rc)));
	}
}
