package com.gentics.mesh.core.endpoint.microschema;

import static com.gentics.mesh.example.ExampleUuids.MICROSCHEMA_UUID;
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
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

public class MicroschemaEndpoint extends AbstractInternalEndpoint {

	private MicroschemaCrudHandler crudHandler;

	public MicroschemaEndpoint() {
		super("microschemas", null);
	}

	@Inject
	public MicroschemaEndpoint(MeshAuthChain chain, MicroschemaCrudHandler crudHandler) {
		super("microschemas", chain);
		this.crudHandler = crudHandler;
	}

	@Override
	public String getDescription() {
		return "Endpoint which provides methods to manipulate microschemas.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addDiffHandler();
		addChangesHandler();

		addCreateHandler();
		addReadHandlers();
		addUpdateHandler();
		addDeleteHandler();

	}

	private void addDiffHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:microschemaUuid/diff");
		endpoint.addUriParameter("microschemaUuid", "Uuid of the microschema.", MICROSCHEMA_UUID);
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(microschemaExamples.getGeolocationMicroschemaCreateRequest());
		endpoint.exampleResponse(OK, schemaExamples.getSchemaChangesListModel(), "Found difference between both microschemas.");
		endpoint.description(
			"Compare the provided schema with the schema which is currently stored and generate a set of changes that have been detected.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String schemaUuid = ac.getParameter("microschemaUuid");
			crudHandler.handleDiff(ac, schemaUuid);
		});
	}

	private void addChangesHandler() {
		// Endpoint readChanges = createEndpoint();
		// readChanges.path("/:schemaUuid/changes");
		// readChanges.method(GET);
		// readChanges.produces(APPLICATION_JSON);
		// readChanges.description("Load all changes that have been applied to the schema.");
		// readChanges.exampleResponse(OK, model)
		// readChanges.handler(rc -> {
		// InternalActionContext ac = InternalActionContext.create(rc);
		// String schemaUuid = ac.getParameter("schemaUuid");
		// crudHandler.handleGetSchemaChanges(ac, schemaUuid);
		// });

		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:microschemaUuid/changes");
		endpoint.addUriParameter("microschemaUuid", "Uuid of the microschema.", MICROSCHEMA_UUID);
		endpoint.method(POST);
		endpoint.produces(APPLICATION_JSON);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.description(
			"Apply the provided changes on the latest version of the schema and migrate all nodes which are based on the schema. Please note that this operation is non-blocking and will continue to run in the background.");
		endpoint.exampleRequest(schemaExamples.getSchemaChangesListModel());
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Microschema migration was invoked.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String schemaUuid = ac.getParameter("microschemaUuid");
			crudHandler.handleApplySchemaChanges(ac, schemaUuid);
		});
	}

	private void addReadHandlers() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:microschemaUuid");
		readOne.addUriParameter("microschemaUuid", "Uuid of the microschema.", MICROSCHEMA_UUID);
		readOne.addQueryParameters(VersioningParametersImpl.class);
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, microschemaExamples.getGeolocationMicroschemaResponse(), "Loaded microschema.");
		readOne.description("Read the microschema with the given uuid.");
		readOne.blockingHandler(rc -> {
			String uuid = rc.request().params().get("microschemaUuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				crudHandler.handleRead(wrap(rc), uuid);
			}
		});

		InternalEndpointRoute readAll = createRoute();
		readAll.path("/");
		readAll.method(GET);
		readAll.description("Read multiple microschemas and return a paged list response.");
		readAll.exampleResponse(OK, microschemaExamples.getMicroschemaListResponse(), "List of miroschemas.");
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.produces(APPLICATION_JSON);
		readAll.blockingHandler(rc -> {
			crudHandler.handleReadList(wrap(rc));
		});
	}

	private void addDeleteHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:microschemaUuid");
		endpoint.addUriParameter("microschemaUuid", "Uuid of the microschema.", MICROSCHEMA_UUID);
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(NO_CONTENT, "Microschema was deleted.");
		endpoint.description("Delete the microschema with the given uuid.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("microschemaUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addUpdateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:microschemaUuid");
		endpoint.addUriParameter("microschemaUuid", "Uuid of the microschema.", MICROSCHEMA_UUID);
		endpoint.method(POST);
		endpoint.produces(APPLICATION_JSON);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.exampleRequest(microschemaExamples.getGeolocationMicroschemaUpdateRequest());
		// endpoint.exampleResponse(OK, microschemaExamples.getGeolocationMicroschemaResponse(), "Updated microschema.");
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "Migration message.");
		endpoint.description("Update the microschema with the given uuid.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("microschemaUuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addCreateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new microschema.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(microschemaExamples.getGeolocationMicroschemaCreateRequest());
		endpoint.exampleResponse(CREATED, microschemaExamples.getGeolocationMicroschemaResponse(), "Created microschema.");
		endpoint.blockingHandler(rc -> {
			crudHandler.handleCreate(wrap(rc));
		});

	}

}
