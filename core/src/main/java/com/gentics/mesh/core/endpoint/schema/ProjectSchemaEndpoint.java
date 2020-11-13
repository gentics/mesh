package com.gentics.mesh.core.endpoint.schema;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_SCHEMA_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_SCHEMA_UNASSIGNED;
import static com.gentics.mesh.example.ExampleUuids.SCHEMA_VEHICLE_UUID;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;

/**
 * Verticle for :apiBase:/PROJECTNAME/schemas
 */
public class ProjectSchemaEndpoint extends AbstractProjectEndpoint {

	private SchemaCrudHandler crudHandler;

	public ProjectSchemaEndpoint() {
		super("schemas", null, null);
	}

	@Inject
	public ProjectSchemaEndpoint(MeshAuthChainImpl chain, BootstrapInitializer boot, SchemaCrudHandler crudHandler) {
		super("schemas", chain, boot);
		this.crudHandler = crudHandler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which can be used to assign schemas to projects.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();
		addReadHandlers();
		addAssignHandler();
		addDeleteHandlers();
	}

	private void addReadHandlers() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:schemaUuid");
		readOne.addUriParameter("schemaUuid", "Uuid of the schema.", SCHEMA_VEHICLE_UUID);
		readOne.method(GET);
		readOne.description("Load the schema with the given uuid.");
		readOne.exampleResponse(OK, schemaExamples.getSchemaResponse(), "Loaded schema.");
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
		readAll.exampleResponse(OK, schemaExamples.getSchemaListResponse(), "Loaded list of schemas.");
		readAll.produces(APPLICATION_JSON);
		readAll.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadProjectList(ac);
		});
	}

	private void addAssignHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:schemaUuid");
		endpoint.addUriParameter("schemaUuid", "Uuid of the schema.", SCHEMA_VEHICLE_UUID);
		endpoint.method(POST);
		endpoint.description(
			"Assign the schema to the project. This will automatically assign the latest schema version to all branches of the project.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, schemaExamples.getSchemaResponse(), "Assigned schema.");
		endpoint.events(PROJECT_SCHEMA_ASSIGNED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("schemaUuid");
			crudHandler.handleAddSchemaToProject(ac, uuid);
		});
	}

	private void addDeleteHandlers() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:schemaUuid");
		endpoint.addUriParameter("schemaUuid", "Uuid of the schema.", SCHEMA_VEHICLE_UUID);
		endpoint.method(DELETE);
		endpoint.description(
			"Remove the schema with the given uuid from the project. This will automatically remove all schema versions of the given schema from all branches of the project.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(NO_CONTENT, "Schema was successfully removed.");
		endpoint.events(PROJECT_SCHEMA_UNASSIGNED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("schemaUuid");
			crudHandler.handleRemoveSchemaFromProject(ac, uuid);
		});
	}
}
