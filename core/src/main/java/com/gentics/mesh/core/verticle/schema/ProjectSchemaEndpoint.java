package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.rest.EndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Verticle for /api/v1/PROJECTNAME/schemas
 */
public class ProjectSchemaEndpoint extends AbstractProjectEndpoint {

	private SchemaCrudHandler crudHandler;

	public ProjectSchemaEndpoint() {
		super("schemas", null);
	}

	@Inject
	public ProjectSchemaEndpoint(BootstrapInitializer boot, SchemaCrudHandler crudHandler) {
		super("schemas", boot);
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
		EndpointRoute readOne = createEndpoint();
		readOne.path("/:schemaUuid");
		readOne.addUriParameter("schemaUuid", "Uuid of the schema.", UUIDUtil.randomUUID());
		readOne.method(GET);
		readOne.description("Load the schema with the given uuid.");
		readOne.exampleResponse(OK, schemaExamples.getSchemaResponse(), "Loaded schema.");
		readOne.produces(APPLICATION_JSON);
		readOne.handler(rc -> {
			String uuid = rc.request().params().get("schemaUuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				crudHandler.handleRead(ac, uuid);
			}
		});

		EndpointRoute readAll = createEndpoint();
		readAll.path("/");
		readAll.method(GET);
		readAll.description("Read multiple schemas and return a paged list response.");
		readAll.exampleResponse(OK, schemaExamples.getSchemaListResponse(), "Loaded list of schemas.");
		readAll.produces(APPLICATION_JSON);
		readAll.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			crudHandler.handleReadProjectList(ac);
		});
	}

	private void addAssignHandler() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/:schemaUuid");
		endpoint.addUriParameter("schemaUuid", "Uuid of the schema.", UUIDUtil.randomUUID());
		endpoint.method(POST);
		endpoint.description(
				"Assign the schema to the project. This will automatically assign the latest schema version to all releases of the project.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, schemaExamples.getSchemaResponse(), "Assigned schema.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("schemaUuid");
			crudHandler.handleAddSchemaToProject(ac, uuid);
		});
	}

	private void addDeleteHandlers() {
		EndpointRoute endpoint = createEndpoint();
		endpoint.path("/:schemaUuid");
		endpoint.addUriParameter("schemaUuid", "Uuid of the schema.", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.description(
				"Remove the schema with the given uuid from the project. This will automatically remove all schema versions of the given schema from all releases of the project.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(NO_CONTENT, "Schema was successfully removed.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("schemaUuid");
			crudHandler.handleRemoveSchemaFromProject(ac, uuid);
		});
	}
}
