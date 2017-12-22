package com.gentics.mesh.core.verticle.admin;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.rest.EndpointRoute;
import com.gentics.mesh.router.route.AbstractEndpoint;
import com.gentics.mesh.util.UUIDUtil;

/**
 * The admin verticle provides core administration rest endpoints.
 */
public class AdminEndpoint extends AbstractEndpoint {

	private AdminHandler adminHandler;

	private JobHandler jobHandler;

	private ConsistencyCheckHandler consistencyHandler;

	@Inject
	public AdminEndpoint(AdminHandler adminHandler, JobHandler jobHandler, ConsistencyCheckHandler consistencyHandler) {
		super("admin");
		this.adminHandler = adminHandler;
		this.jobHandler = jobHandler;
		this.consistencyHandler = consistencyHandler;
	}

	public AdminEndpoint() {
		super("admin");
	}

	@Override
	public String getDescription() {
		// TODO what is a admin permission?
		return "Collection of administrative endpoints which usually require admin permission";
	}

	@Override
	public void registerEndPoints() {
		addMeshStatusHandler();

		secureAll();

		addBackupHandler();
		addRestoreHandler();
		addClusterStatusHandler();
		addConsistencyCheckHandler();
		// addImportHandler();
		// addExportHandler();
		// addVerticleHandler();
		// addServiceHandler();
		addJobHandler();

	}

	private void addClusterStatusHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/cluster/status");
		endpoint.method(GET);
		endpoint.description("Loads the cluster status information.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, adminExamples.createClusterStatusResponse(), "Cluster status.");
		endpoint.handler(rc -> {
			adminHandler.handleClusterStatus(new InternalRoutingActionContextImpl(rc));
		});

	}

	private void addConsistencyCheckHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/consistency/check");
		endpoint.method(GET);
		endpoint.description("Invokes a consistency check of the graph database and returns a list of found issues");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.createConsistencyCheckResponse(), "Consistency check report");
		endpoint.handler(rc -> {
			consistencyHandler.invokeCheck(new InternalRoutingActionContextImpl(rc));
		});
	}

	private void addExportHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/graphdb/export");
		endpoint.method(POST);
		endpoint.description("Invoke a orientdb graph database export.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Export process was invoked.");
		endpoint.handler(rc -> {
			adminHandler.handleExport(new InternalRoutingActionContextImpl(rc));
		});
	}

	private void addImportHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/graphdb/import");
		endpoint.method(POST);
		endpoint.description(
				"Invoke a orientdb graph database import. The latest import file from the import directory will be used for this operation.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Database import command was invoked.");
		endpoint.handler(rc -> {
			adminHandler.handleImport(new InternalRoutingActionContextImpl(rc));
		});
	}

	private void addRestoreHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/graphdb/restore");
		endpoint.description(
				"Invoke a graph database restore. The latest dump from the backup directory will be inserted. Please note that this operation will block all current operation and effecivly destroy all previously stored data.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Database restore command was invoked.");
		endpoint.method(POST);
		endpoint.handler(rc -> {
			adminHandler.handleRestore(new InternalRoutingActionContextImpl(rc));
		});
	}

	private void addBackupHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/graphdb/backup");
		endpoint.method(POST);
		endpoint.description(
				"Invoke a graph database backup and dump the data to the configured backup location. Note that this operation will block all current operation.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Incremental backup was invoked.");
		endpoint.handler(rc -> {
			adminHandler.handleBackup(new InternalRoutingActionContextImpl(rc));
		});
	}

	/**
	 * Handler that reacts onto status requests.
	 */
	private void addMeshStatusHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.description("Return the Gentics Mesh server status.");
		endpoint.path("/status");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, adminExamples.createMeshStatusResponse(MeshStatus.READY), "Status of the Gentics Mesh server.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			// TODO this is currently polled by apa. We need to update their monitoring as well if we change this
			adminHandler.handleMeshStatus(ac);
		});

	}

	// private void addServiceHandler() {
	// route("/deployService/:mavenCoordinates").method(GET).handler(rc -> {
	// // TODO impl me
	// rc.response().end("Deploy " + rc.request().params().get("mavenCoordinates"));
	// });
	//
	// route("/undeployService/:mavenCoordinates").method(GET).handler(rc -> {
	// // TODO impl me
	// rc.response().end("Undeploy " + rc.request().params().get("mavenCoordinates"));
	// });
	//
	// }
	//
	// private void addVerticleHandler() {
	// route("/deployVerticle/:clazz").method(GET).handler(rc -> {
	// String clazz = rc.request().params().get("clazz");
	// try {
	// // TODO create merged jsonconfig (see mesh init)
	// JsonObject config = new JsonObject();
	// String id = deployAndWait(vertx, config, clazz);
	// rc.response().end("Deployed " + clazz + " id: " + id);
	// } catch (Exception e) {
	// rc.fail(e);
	// }
	// });
	//
	// route("/undeployVerticle/:clazz").method(GET).handler(rc -> {
	// // TODO impl me
	// rc.response().end("Undeploy " + rc.request().params().get("clazz"));
	// });
	// }

	private void addJobHandler() {

		EndpointRoute invokeJobWorker = createEndpoint();
		invokeJobWorker.path("/processJobs");
		invokeJobWorker.method(POST);
		invokeJobWorker.description("Invoke the processing of remaining jobs.");
		invokeJobWorker.produces(APPLICATION_JSON);
		invokeJobWorker.exampleResponse(OK, miscExamples.createMessageResponse(), "Response message.");
		invokeJobWorker.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			jobHandler.handleInvokeJobWorker(ac);
		});

		EndpointRoute readJobList = createEndpoint();
		readJobList.path("/jobs");
		readJobList.method(GET);
		readJobList.description("List all currently queued jobs.");
		readJobList.produces(APPLICATION_JSON);
		readJobList.exampleResponse(OK, jobExamples.createJobList(), "List of jobs.");
		readJobList.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			jobHandler.handleReadList(ac);
		});

		EndpointRoute readJob = createEndpoint();
		readJob.path("/jobs/:jobUuid");
		readJob.method(GET);
		readJob.description("Load a specific job.");
		readJob.produces(APPLICATION_JSON);
		readJob.addUriParameter("jobUuid", "Uuid of the job.", UUIDUtil.randomUUID());
		readJob.exampleResponse(OK, jobExamples.createJobResponse(), "Job information.");
		readJob.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("jobUuid");
			jobHandler.handleRead(ac, uuid);
		});

		EndpointRoute deleteJob = createEndpoint();
		deleteJob.path("/jobs/:jobUuid");
		deleteJob.method(DELETE);
		deleteJob.description("Deletes the job. Note that it is only possible to delete failed jobs");
		deleteJob.addUriParameter("jobUuid", "Uuid of the job.", UUIDUtil.randomUUID());
		deleteJob.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("jobUuid");
			jobHandler.handleDelete(ac, uuid);
		});

		EndpointRoute resetJob = createEndpoint();
		resetJob.path("/jobs/:jobUuid/error");
		resetJob.method(DELETE);
		resetJob.description("Deletes error state from the job. This will make it possible to execute the job once again.");
		resetJob.addUriParameter("jobUuid", "Uuid of the job.", UUIDUtil.randomUUID());
		resetJob.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("jobUuid");
			jobHandler.handleResetJob(ac, uuid);
		});
	}

}
