package com.gentics.mesh.core.endpoint.schema;

import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_BRANCH_ASSIGN;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_UPDATED;
import static com.gentics.mesh.example.ExampleUuids.SCHEMA_VEHICLE_UUID;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

/**
 * Verticle for /api/v2/schemas endpoint
 */
public class SchemaEndpoint extends AbstractInternalEndpoint {

	private SchemaCrudHandler crudHandler;

	private SchemaLock schemaLock;

	public SchemaEndpoint() {
		super("schemas", null);
	}

	@Inject
	public SchemaEndpoint(MeshAuthChain chain, SchemaCrudHandler crudHandler, SchemaLock schemaLock) {
		super("schemas", chain);
		this.crudHandler = crudHandler;
		this.schemaLock = schemaLock;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of schemas.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addReadHandlers();
		addDiffHandler();
		addChangesHandler();
		addCreateHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addChangesHandler() {
		// Endpoint readChanges = createEndpoint();
		// readChanges.path("/:schemaUuid/changes");
		// readChanges.method(GET);
		// readChanges.description("Return a list of changes ");
		// readChanges.produces(APPLICATION_JSON);
		// readChanges.exampleResponse(OK, schemaExamples.)
		// readChanges.handler(rc -> {
		// crudHandler.handleGetSchemaChanges(InternalActionContext.create(rc));
		// });

		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:schemaUuid/changes");
		endpoint.addUriParameter("schemaUuid", "Uuid of the schema.", SCHEMA_VEHICLE_UUID);
		endpoint.method(POST);
		endpoint.description("Apply the posted changes to the schema. The schema migration will not automatically be started.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(schemaExamples.getSchemaChangesListModel());
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Schema changes have been applied.");
		endpoint.events(SCHEMA_UPDATED, SCHEMA_BRANCH_ASSIGN, SCHEMA_MIGRATION_START, SCHEMA_MIGRATION_FINISHED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String schemaUuid = ac.getParameter("schemaUuid");
			crudHandler.handleApplySchemaChanges(ac, schemaUuid);
		});
	}

	private void addCreateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new schema.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(schemaExamples.getSchemaCreateRequest());
		endpoint.exampleResponse(CREATED, schemaExamples.getSchemaResponse(), "Created schema.");
		endpoint.events(SCHEMA_CREATED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleCreate(ac);
		});
	}

	private void addDiffHandler() {
		InternalEndpointRoute diffEndpoint = createRoute();
		diffEndpoint.path("/:schemaUuid/diff");
		diffEndpoint.addUriParameter("schemaUuid", "Uuid of the schema.", SCHEMA_VEHICLE_UUID);
		diffEndpoint.method(POST);
		diffEndpoint.description("Compare the given schema with the stored schema and create a changeset.");
		diffEndpoint.consumes(APPLICATION_JSON);
		diffEndpoint.produces(APPLICATION_JSON);
		diffEndpoint.exampleRequest(schemaExamples.getSchemaResponse());
		diffEndpoint.exampleResponse(OK, schemaExamples.getSchemaChangesListModel(),
			"List of schema changes that were detected by comparing the posted schema and the current version.");
		diffEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("schemaUuid");
			crudHandler.handleDiff(ac, uuid);
		});
	}

	private void addUpdateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:schemaUuid");
		endpoint.addUriParameter("schemaUuid", "Uuid of the schema.", SCHEMA_VEHICLE_UUID);
		endpoint.method(POST);
		endpoint.description("Update the schema.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.addQueryParameters(SchemaUpdateParametersImpl.class);
		endpoint.exampleRequest(schemaExamples.getSchemaUpdateRequest());
		endpoint.exampleResponse(OK, schemaExamples.getSchemaResponse(), "Updated schema.");
		endpoint.events(SCHEMA_UPDATED, SCHEMA_MIGRATION_START, SCHEMA_MIGRATION_FINISHED);
		endpoint.blockingHandler(rc -> {
			// Update operations should always be executed sequentially - never in parallel
			synchronized (schemaLock.mutex()) {
				InternalActionContext ac = wrap(rc);
				String uuid = ac.getParameter("schemaUuid");
				crudHandler.handleUpdate(ac, uuid);
			}
		}, true);
	}

	private void addDeleteHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:schemaUuid");
		endpoint.addUriParameter("schemaUuid", "Uuid of the schema.", SCHEMA_VEHICLE_UUID);
		endpoint.method(DELETE);
		endpoint.description("Delete the schema with the given uuid.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(NO_CONTENT, "Schema was successfully deleted.");
		endpoint.events(SCHEMA_DELETED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("schemaUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addReadHandlers() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:schemaUuid");
		readOne.addUriParameter("schemaUuid", "Uuid of the schema.", SCHEMA_VEHICLE_UUID);
		readOne.method(GET);
		readOne.addQueryParameters(VersioningParametersImpl.class);
		readOne.description("Load the schema with the given uuid.");
		readOne.exampleResponse(OK, schemaExamples.getSchemaResponse(), "Loaded schema.");
		readOne.addQueryParameters(GenericParametersImpl.class);
		readOne.produces(APPLICATION_JSON);
		readOne.blockingHandler(rc -> {
			String uuid = rc.request().params().get("schemaUuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				InternalActionContext ac = wrap(rc);
				crudHandler.handleRead(ac, uuid);
			}
		});

		InternalEndpointRoute readAll = createRoute();
		readAll.path("/");
		readAll.method(GET);
		readAll.description("Read multiple schemas and return a paged list response.");
		readAll.produces(APPLICATION_JSON);
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.addQueryParameters(GenericParametersImpl.class);
		readAll.exampleResponse(OK, schemaExamples.getSchemaListResponse(), "Loaded list of schemas.");
		readAll.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadList(ac);
		});

	}
}
