package com.gentics.mesh.core.verticle.microschema;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.rest.Endpoint;

@Component
@Scope("singleton")
@SpringVerticle
public class MicroschemaVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private MicroschemaCrudHandler crudHandler;

	public MicroschemaVerticle() {
		super("microschemas");
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

	private void addDiffHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid/diff");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(microschemaExamples.getGeolocationMicroschema());
		endpoint.exampleResponse(200, schemaExamples.getSchemaChangesListModel());
		endpoint.description(
				"Compare the provided schema with the schema which is currently stored and generate a set of changes that have been detected.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String schemaUuid = ac.getParameter("uuid");
			crudHandler.handleDiff(ac, schemaUuid);
		});
	}

	private void addChangesHandler() {
		//		Endpoint readChanges = createEndpoint();
		//		readChanges.path("/:schemaUuid/changes");
		//		readChanges.method(GET);
		//		readChanges.produces(APPLICATION_JSON);
		//		readChanges.description("Load all changes that have been applied to the schema.");
		//		readChanges.exampleResponse(200, model)
		//		readChanges.handler(rc -> {
		//			InternalActionContext ac = InternalActionContext.create(rc);
		//			String schemaUuid = ac.getParameter("schemaUuid");
		//			crudHandler.handleGetSchemaChanges(ac, schemaUuid);
		//		});

		Endpoint executeChanges = createEndpoint();
		executeChanges.path("/:schemaUuid/changes");
		executeChanges.method(POST);
		executeChanges.produces(APPLICATION_JSON);
		executeChanges.consumes(APPLICATION_JSON);
		executeChanges.description(
				"Apply the provided changes on the latest version of the schema and migrate all nodes which are based on the schema. Please note that this operation is non-blocking and will continue to run in the background.");
		executeChanges.exampleRequest(schemaExamples.getSchemaChangesListModel());
		executeChanges.exampleResponse(200, miscExamples.getMessageResponse());
		executeChanges.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String schemaUuid = ac.getParameter("schemaUuid");
			crudHandler.handleApplySchemaChanges(ac, schemaUuid);
		});
	}

	private void addReadHandlers() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:uuid");
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(200, microschemaExamples.getGeolocationMicroschema());
		readOne.description("Read the microschema with the given uuid.");
		readOne.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				InternalActionContext ac = InternalActionContext.create(rc);
				crudHandler.handleRead(ac, uuid);
			}
		});

		Endpoint readAll = createEndpoint();
		readAll.path("/");
		readAll.method(GET);
		readAll.description("Read multiple microschemas and return a paged list response.");
		readAll.exampleResponse(200, microschemaExamples.getMicroschemaListResponse());
		readAll.produces(APPLICATION_JSON);
		readAll.handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	private void addDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid");
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
		endpoint.description("Delete the microschema with the given uuid.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addUpdateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid");
		endpoint.method(PUT);
		endpoint.produces(APPLICATION_JSON);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.exampleRequest(microschemaExamples.getGeolocationMicroschema());
		endpoint.exampleResponse(200, microschemaExamples.getGeolocationMicroschema());
		endpoint.description("Update the microschema with the given uuid.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addCreateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new microschema.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(microschemaExamples.getGeolocationMicroschemaCreateRequest());
		endpoint.exampleResponse(200, microschemaExamples.getGeolocationMicroschema());
		endpoint.handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});

	}

}
