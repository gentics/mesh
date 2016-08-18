package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Verticle for /api/v1/schemas endpoint
 */
@Component
@Scope("singleton")
@SpringVerticle
public class SchemaVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private SchemaContainerCrudHandler crudHandler;

	public SchemaVerticle() {
		super("schemas");
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of schemas.";
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		addDiffHandler();
		addChangesHandler();

		addCreateHandler();
		addReadHandlers();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addChangesHandler() {
		//		Endpoint readChanges = createEndpoint();
		//		readChanges.path("/:schemaUuid/changes");
		//		readChanges.method(GET);
		//		readChanges.description("Return a list of changes ");
		//		readChanges.produces(APPLICATION_JSON);
		//		readChanges.exampleResponse(OK, schemaExamples.)
		//		readChanges.handler(rc -> {
		//			crudHandler.handleGetSchemaChanges(InternalActionContext.create(rc));
		//		});

		Endpoint endpoint = createEndpoint();
		endpoint.path("/:schemaUuid/changes");
		endpoint.addUriParameter("schemaUuid", "Uuid of the schema.", UUIDUtil.randomUUID());
		endpoint.method(POST);
		endpoint.description("Apply the posted changes to the schema.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(schemaExamples.getSchemaChangesListModel());
		endpoint.exampleResponse(OK, miscExamples.getMessageResponse(), "Schema migration was started.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String schemaUuid = ac.getParameter("schemaUuid");
			crudHandler.handleApplySchemaChanges(ac, schemaUuid);
		});
	}

	private void addCreateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new schema.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(schemaExamples.getSchemaCreateRequest());
		endpoint.exampleResponse(CREATED, schemaExamples.getSchema(), "Created schema.");
		endpoint.handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}

	private void addDiffHandler() {
		Endpoint diffEndpoint = createEndpoint();
		diffEndpoint.path("/:schemaUuid/diff");
		diffEndpoint.addUriParameter("schemaUuid", "Uuid of the schema.", UUIDUtil.randomUUID());
		diffEndpoint.method(POST);
		diffEndpoint.description("Compare the given schema with the stored schema and create a changeset.");
		diffEndpoint.consumes(APPLICATION_JSON);
		diffEndpoint.produces(APPLICATION_JSON);
		diffEndpoint.exampleRequest(schemaExamples.getSchema());
		diffEndpoint.exampleResponse(OK, schemaExamples.getSchemaChangesListModel(),
				"List of schema changes that were detected by comparing the posted schema and the current version.");
		diffEndpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("schemaUuid");
			crudHandler.handleDiff(ac, uuid);
		});
	}

	private void addUpdateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:schemaUuid");
		endpoint.addUriParameter("schemaUuid", "Uuid of the schema.", UUIDUtil.randomUUID());
		endpoint.method(POST);
		endpoint.description("Update the schema.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(schemaExamples.getSchemaUpdateRequest());
		endpoint.exampleResponse(OK, schemaExamples.getSchema(), "Updated schema.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("schemaUuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:schemaUuid");
		endpoint.addUriParameter("schemaUuid", "Uuid of the schema.", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.description("Delete the schema with the given uuid.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(NO_CONTENT, "Schema was successfully deleted.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("schemaUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addReadHandlers() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:schemaUuid");
		readOne.addUriParameter("schemaUuid", "Uuid of the schema.", UUIDUtil.randomUUID());
		readOne.method(GET);
		readOne.description("Load the schema with the given uuid.");
		readOne.exampleResponse(OK, schemaExamples.getSchema(), "Loaded schema.");
		readOne.produces(APPLICATION_JSON);
		readOne.handler(rc -> {
			String uuid = rc.request().params().get("schemaUuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				crudHandler.handleRead(InternalActionContext.create(rc), uuid);
			}
		});

		Endpoint readAll = createEndpoint();
		readAll.path("/");
		readAll.method(GET);
		readAll.description("Read multiple schemas and return a paged list response.");
		readAll.produces(APPLICATION_JSON);
		readAll.addQueryParameters(PagingParameters.class);
		readAll.exampleResponse(OK, schemaExamples.getSchemaListResponse(), "Loaded list of schemas.");
		readAll.handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});

	}
}
