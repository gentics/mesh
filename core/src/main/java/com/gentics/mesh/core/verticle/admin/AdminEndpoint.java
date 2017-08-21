package com.gentics.mesh.core.verticle.admin;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.AbstractEndpoint;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.Endpoint;

/**
 * The admin verticle provides core administration rest endpoints.
 */
@Singleton
public class AdminEndpoint extends AbstractEndpoint {

	private AdminHandler handler;

	@Inject
	public AdminEndpoint(RouterStorage routerStorage, AdminHandler adminHandler) {
		super("admin", routerStorage);
		this.handler = adminHandler;
	}

	public AdminEndpoint() {
		super("admin", null);
	}

	@Override
	public String getDescription() {
		// TODO what is a admin permission?
		return "Collection of administrative endpoints which usually require admin permission";
	}

	@Override
	public void registerEndPoints() {
		addMeshStatusHandler();
		addMigrationStatusHandler();

		secureAll();
		addBackupHandler();
		addRestoreHandler();
		//addImportHandler();
		//addExportHandler();
		// addVerticleHandler();
		// addServiceHandler();

	}

	private void addMigrationStatusHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/status/migrations");
		endpoint.method(GET);
		endpoint.description("Return the current schema or node migration status.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.getMessageResponse(), "Migration status.");
		endpoint.handler(rc -> {
			handler.handleMigrationStatus(new InternalRoutingActionContextImpl(rc));
		});
	}

	private void addExportHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/graphdb/export");
		endpoint.method(POST);
		endpoint.description("Invoke a orientdb graph database export.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.getMessageResponse(), "Export process was invoked.");
		endpoint.handler(rc -> {
			handler.handleExport(new InternalRoutingActionContextImpl(rc));
		});
	}

	private void addImportHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/graphdb/import");
		endpoint.method(POST);
		endpoint.description(
				"Invoke a orientdb graph database import. The latest import file from the import directory will be used for this operation.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.getMessageResponse(), "Database import command was invoked.");
		endpoint.handler(rc -> {
			handler.handleImport(new InternalRoutingActionContextImpl(rc));
		});
	}

	private void addRestoreHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/graphdb/restore");
		endpoint.description(
				"Invoke a graph database restore. The latest dump from the backup directory will be inserted. Please note that this operation will block all current operation and effecivly destroy all previously stored data.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.getMessageResponse(), "Database restore command was invoked.");
		endpoint.method(POST);
		endpoint.handler(rc -> {
			handler.handleRestore(new InternalRoutingActionContextImpl(rc));
		});
	}

	private void addBackupHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/graphdb/backup");
		endpoint.method(POST);
		endpoint.description(
				"Invoke a graph database backup and dump the data to the configured backup location. Note that this operation will block all current operation.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, miscExamples.getMessageResponse(), "Incremental backup was invoked.");
		endpoint.handler(rc -> {
			handler.handleBackup(new InternalRoutingActionContextImpl(rc));
		});
	}

	/**
	 * Handler that reacts onto status requests.
	 */
	private void addMeshStatusHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.description("Return the Gentics Mesh server status.");
		endpoint.path("/status");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, adminExamples.createMeshStatusResponse(MeshStatus.READY), "Status of the Gentics Mesh server.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			//TODO this is currently polled by apa. We need to update their monitoring as well if we change this
			handler.handleMeshStatus(ac);
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

}
