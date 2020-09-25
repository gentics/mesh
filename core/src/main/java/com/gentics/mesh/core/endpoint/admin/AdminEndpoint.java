package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.context.InternalActionContext.internalHandler;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_BACKUP_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_BACKUP_START;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_EXPORT_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_EXPORT_START;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_IMPORT_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_IMPORT_START;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_RESTORE_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.GRAPH_RESTORE_START;
import static com.gentics.mesh.core.rest.MeshEvent.PLUGIN_DEPLOYED;
import static com.gentics.mesh.core.rest.MeshEvent.PLUGIN_DEPLOYING;
import static com.gentics.mesh.core.rest.MeshEvent.PLUGIN_UNDEPLOYED;
import static com.gentics.mesh.core.rest.MeshEvent.PLUGIN_UNDEPLOYING;
import static com.gentics.mesh.core.rest.MeshEvent.REPAIR_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.REPAIR_START;
import static com.gentics.mesh.example.ExampleUuids.JOB_UUID;
import static com.gentics.mesh.example.ExampleUuids.PLUGIN_1_ID;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoHandler;
import com.gentics.mesh.core.endpoint.admin.plugin.PluginHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

/**
 * The admin verticle provides core administration rest endpoints.
 */
public class AdminEndpoint extends AbstractInternalEndpoint {

	private AdminHandler adminHandler;

	private JobHandler jobHandler;

	private ConsistencyCheckHandler consistencyHandler;

	private PluginHandler pluginHandler;

	private DebugInfoHandler debugInfoHandler;

	private LocalConfigHandler localConfigHandler;

	private ShutdownHandler shutdownHandler;

	private HandlerUtilities handlerUtilities;

	@Inject
	public AdminEndpoint(MeshAuthChain chain, AdminHandler adminHandler, JobHandler jobHandler, ConsistencyCheckHandler consistencyHandler,
		PluginHandler pluginHandler, DebugInfoHandler debugInfoHandler, LocalConfigHandler localConfigHandler, ShutdownHandler shutdownHandler,
		HandlerUtilities handlerUtilities) {
		super("admin", chain);
		this.adminHandler = adminHandler;
		this.jobHandler = jobHandler;
		this.consistencyHandler = consistencyHandler;
		this.pluginHandler = pluginHandler;
		this.debugInfoHandler = debugInfoHandler;
		this.localConfigHandler = localConfigHandler;
		this.shutdownHandler = shutdownHandler;
		this.handlerUtilities = handlerUtilities;
	}

	public AdminEndpoint() {
		super("admin", null);
	}

	@Override
	public String getDescription() {
		// TODO what is an admin permission?
		return "Collection of administrative endpoints which usually require admin permission";
	}

	@Override
	public void registerEndPoints() {
		addMeshStatusHandler();

		secureAll();

		addSecurityLogger();

		addBackupHandler();
		addRestoreHandler();
		addClusterStatusHandler();
		addClusterConfigHandler();
		addConsistencyCheckHandler();
		addImportHandler();
		addExportHandler();
		// addVerticleHandler();
		// addServiceHandler();
		addJobHandler();
		addPluginHandler();
		addDebugInfoHandler();
		addRuntimeConfigHandler();
		addShutdownHandler();
		addCoordinatorHandler();
	}

	private void addSecurityLogger() {
		getRouter().route().handler(internalHandler((rc, ac) -> {
			ac.getSecurityLogger().info("Accessed path " + rc.request().path());
			rc.next();
		}));
	}

	private void addPluginHandler() {
		InternalEndpointRoute deployEndpoint = createRoute();
		deployEndpoint.path("/plugins");
		deployEndpoint.method(POST);
		deployEndpoint.description("Deploys the plugin using the provided deployment information.");
		deployEndpoint.produces(APPLICATION_JSON);
		deployEndpoint.exampleRequest(adminExamples.createPluginDeploymentRequest());
		deployEndpoint.exampleResponse(OK, adminExamples.createHelloWorldPluginResponse(), "Plugin response.");
		deployEndpoint.events(PLUGIN_DEPLOYED, PLUGIN_DEPLOYING);
		deployEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			pluginHandler.handleDeploy(ac);
		});

		InternalEndpointRoute undeployEndpoint = createRoute();
		undeployEndpoint.path("/plugins/:id");
		undeployEndpoint.method(DELETE);
		undeployEndpoint.description("Undeploys the plugin with the given uuid.");
		undeployEndpoint.produces(APPLICATION_JSON);
		undeployEndpoint.addUriParameter("id", "Id of the plugin.", PLUGIN_1_ID);
		undeployEndpoint.exampleResponse(OK, adminExamples.createHelloWorldPluginResponse(), "Plugin response.");
		undeployEndpoint.events(PLUGIN_UNDEPLOYED, PLUGIN_UNDEPLOYING);
		undeployEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("id");
			pluginHandler.handleUndeploy(ac, uuid);
		});

		InternalEndpointRoute readEndpoint = createRoute();
		readEndpoint.path("/plugins/:uuid");
		readEndpoint.method(GET);
		readEndpoint.description("Loads deployment information for the plugin with the given id.");
		readEndpoint.produces(APPLICATION_JSON);
		readEndpoint.addUriParameter("uuid", "Uuid of the plugin.", PLUGIN_1_ID);
		readEndpoint.exampleResponse(OK, adminExamples.createHelloWorldPluginResponse(), "Plugin response.");
		readEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("uuid");
			pluginHandler.handleRead(ac, uuid);
		});

		InternalEndpointRoute readAllEndpoint = createRoute();
		readAllEndpoint.path("/plugins");
		readAllEndpoint.method(GET);
		readAllEndpoint.description("Loads deployment information for all deployed plugins.");
		readAllEndpoint.produces(APPLICATION_JSON);
		readAllEndpoint.exampleResponse(OK, adminExamples.createPluginListResponse(), "Plugin list response.");
		readAllEndpoint.blockingHandler(rc -> {
			pluginHandler.handleReadList(wrap(rc));
		});
	}

	/**
	 * @deprecated Use monitoring server endpoint instead
	 */
	@Deprecated
	private void addClusterStatusHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/cluster/status");
		endpoint.method(GET);
		endpoint.description("Loads the cluster status information.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, adminExamples.createClusterStatusResponse(), "Cluster status.");
		endpoint.blockingHandler(rc -> {
			adminHandler.handleClusterStatus(wrap(rc));
		});
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

	private void addConsistencyCheckHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/consistency/check");
		endpoint.method(GET);
		endpoint.description(
			"Invokes a consistency check of the graph database without attempting to repairing the found issues. A list of found issues will be returned.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, adminExamples.createConsistencyCheckResponse(false), "Consistency check report");
		endpoint.blockingHandler(rc -> {
			consistencyHandler.invokeCheck(wrap(rc));
		}, false);

		InternalEndpointRoute repairEndpoint = createRoute();
		repairEndpoint.path("/consistency/repair");
		repairEndpoint.method(POST);
		repairEndpoint
			.description("Invokes a consistency check and repair of the graph database and returns a list of found issues and their state.");
		repairEndpoint.produces(APPLICATION_JSON);
		repairEndpoint.exampleResponse(OK, adminExamples.createConsistencyCheckResponse(true), "Consistency check and repair report");
		repairEndpoint.events(REPAIR_START, REPAIR_FINISHED);
		repairEndpoint.blockingHandler(rc -> {
			consistencyHandler.invokeRepair(wrap(rc));
		});
	}

	private void addExportHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/graphdb/export");
		endpoint.method(POST);
		endpoint.setMutating(false);
		endpoint.description("Invoke a orientdb graph database export.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Export process was invoked.");
		endpoint.events(GRAPH_EXPORT_START, GRAPH_EXPORT_FINISHED);
		endpoint.blockingHandler(rc -> {
			adminHandler.handleExport(wrap(rc));
		});
	}

	private void addImportHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/graphdb/import");
		endpoint.method(POST);
		endpoint.description(
			"Invoke a orientdb graph database import. The latest import file from the import directory will be used for this operation.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Database import command was invoked.");
		endpoint.events(GRAPH_IMPORT_START, GRAPH_IMPORT_FINISHED);
		endpoint.blockingHandler(rc -> {
			adminHandler.handleImport(wrap(rc));
		});
	}

	private void addRestoreHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/graphdb/restore");
		endpoint.description(
			"Invoke a graph database restore. The latest dump from the backup directory will be inserted. Please note that this operation will block all current operation and effectively destroy all previously stored data.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Database restore command was invoked.");
		endpoint.method(POST);
		endpoint.events(GRAPH_RESTORE_START, GRAPH_RESTORE_FINISHED);
		endpoint.blockingHandler(rc -> {
			adminHandler.handleRestore(wrap(rc));
		});
	}

	private void addBackupHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/graphdb/backup");
		endpoint.method(POST);
		endpoint.setMutating(false);
		endpoint.description(
			"Invoke a graph database backup and dump the data to the configured backup location. Note that this operation will block all current operation.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Incremental backup was invoked.");
		endpoint.events(GRAPH_BACKUP_START, GRAPH_BACKUP_FINISHED);
		endpoint.blockingHandler(rc -> {
			adminHandler.handleBackup(wrap(rc));
		});
	}

	/**
	 * Handler that reacts onto status requests.
	 * 
	 * @deprecated Use monitoring server status endpoint instead
	 */
	@Deprecated
	private void addMeshStatusHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.description("Return the Gentics Mesh server status.");
		endpoint.path("/status");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, adminExamples.createMeshStatusResponse(MeshStatus.READY), "Status of the Gentics Mesh server.");
		endpoint.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			// TODO this is currently polled by apa. We need to update their monitoring as well if we change this
			adminHandler.handleMeshStatus(ac);
		});

	}

	private void addJobHandler() {

		InternalEndpointRoute invokeJobWorker = createRoute();
		invokeJobWorker.path("/processJobs");
		invokeJobWorker.method(POST);
		invokeJobWorker.description("Invoke the processing of remaining jobs.");
		invokeJobWorker.produces(APPLICATION_JSON);
		invokeJobWorker.exampleResponse(OK, miscExamples.createMessageResponse(), "Response message.");
		invokeJobWorker.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			jobHandler.handleInvokeJobWorker(ac);
		});

		InternalEndpointRoute readJobList = createRoute();
		readJobList.path("/jobs");
		readJobList.method(GET);
		readJobList.description("List all currently queued jobs.");
		readJobList.produces(APPLICATION_JSON);
		readJobList.exampleResponse(OK, jobExamples.createJobList(), "List of jobs.");
		readJobList.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			jobHandler.handleReadList(ac);
		});

		InternalEndpointRoute readJob = createRoute();
		readJob.path("/jobs/:jobUuid");
		readJob.method(GET);
		readJob.description("Load a specific job.");
		readJob.produces(APPLICATION_JSON);
		readJob.addUriParameter("jobUuid", "Uuid of the job.", JOB_UUID);
		readJob.exampleResponse(OK, jobExamples.createJobResponse(), "Job information.");
		readJob.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("jobUuid");
			jobHandler.handleRead(ac, uuid);
		});

		InternalEndpointRoute deleteJob = createRoute();
		deleteJob.path("/jobs/:jobUuid");
		deleteJob.method(DELETE);
		deleteJob.description("Deletes the job. Note that it is only possible to delete failed jobs");
		deleteJob.addUriParameter("jobUuid", "Uuid of the job.", JOB_UUID);
		deleteJob.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("jobUuid");
			jobHandler.handleDelete(ac, uuid);
		});

		InternalEndpointRoute processJob = createRoute();
		processJob.path("/jobs/:jobUuid/process");
		processJob.method(POST);
		processJob.description("Process the job. Failed jobs will be automatically reset and put in queued state.");
		processJob.addUriParameter("jobUuid", "Uuid of the job.", JOB_UUID);
		processJob.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("jobUuid");
			jobHandler.handleProcess(ac, uuid);
		});

		InternalEndpointRoute resetJob = createRoute();
		resetJob.path("/jobs/:jobUuid/error");
		resetJob.method(DELETE);
		resetJob.description("Deletes error state from the job. This will make it possible to execute the job once again.");
		resetJob.addUriParameter("jobUuid", "Uuid of the job.", JOB_UUID);
		resetJob.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("jobUuid");
			jobHandler.handleResetJob(ac, uuid);
		});
	}

	private void addDebugInfoHandler() {
		InternalEndpointRoute route = createRoute();
		route.path("/debuginfo");
		route.method(GET);
		route.description("Downloads a zip file of various [debug information](/docs/administration-guide/#debuginfo) files.");
		route.addQueryParameter("include", "Information to include. See the [documentation](/docs/administration-guide/#debuginfo) for possible values.", "-backup,consistencyCheck");
		route.handler(rc -> debugInfoHandler.handle(rc));
	}

	private void addRuntimeConfigHandler() {
		InternalEndpointRoute getRoute = createRoute();
		getRoute.path("/config");
		getRoute.method(GET);
		getRoute.produces(APPLICATION_JSON);
		getRoute.description("Retrieves the currently active local configuration of this instance.");
		getRoute.exampleResponse(OK, localConfig.createExample(), "The currently active local configuration");
		getRoute.handler(rc -> localConfigHandler.handleGetActiveConfig(wrap(rc)));

		InternalEndpointRoute postRoute = createRoute();
		postRoute.path("/config");
		postRoute.method(POST);
		postRoute.setMutating(false);
		postRoute.produces(APPLICATION_JSON);
		postRoute.description("Sets the currently active local configuration of this instance.");
		postRoute.exampleResponse(OK, localConfig.createExample(), "The currently active local configuration");
		postRoute.handler(rc -> localConfigHandler.handleSetActiveConfig(wrap(rc)));
	}

	private void addShutdownHandler() {
		InternalEndpointRoute postRoute = createRoute();
		postRoute.path("/shutdown");
		postRoute.method(POST);
		postRoute.produces(APPLICATION_JSON);
		postRoute.description("Initiates shutdown of this instance.");
		postRoute.exampleResponse(OK, miscExamples.createMessageResponse(), "Shutdown initiated.");
		postRoute
			.blockingHandler(rc -> handlerUtilities.requiresAdminRole(rc))
			.handler(rc -> shutdownHandler.shutdown(wrap(rc)));
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
