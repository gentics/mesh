package com.gentics.mesh.core.endpoint.microschema;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_MICROSCHEMA_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_MICROSCHEMA_UNASSIGNED;
import static com.gentics.mesh.example.ExampleUuids.MICROSCHEMA_UUID;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;

/**
 * Endpoint for :apiBase:/PROJECTNAME/microschemas
 */
public class ProjectMicroschemaEndpoint extends AbstractProjectEndpoint {

	private MicroschemaCrudHandler crudHandler;

	public ProjectMicroschemaEndpoint() {
		super("microschemas", null, null);
	}

	@Inject
	public ProjectMicroschemaEndpoint(MeshAuthChain chain, BootstrapInitializer boot, MicroschemaCrudHandler crudHandler) {
		super("microschemas", chain, boot);
		this.crudHandler = crudHandler;
	}

	@Override
	public String getDescription() {
		return "Contains endpoints which allow microschemas to be assigned to projects.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();
		addReadHandlers();
		addAssignHandler();
		addDeleteHandlers();
	}

	private void addReadHandlers() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Read all microschemas which are assigned to the project.");
		endpoint.exampleResponse(OK, microschemaExamples.getMicroschemaListResponse(), "List of assigned microschemas.");
		endpoint.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadMicroschemaList(ac);
		});
	}

	private void addAssignHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:microschemaUuid");
		endpoint.addUriParameter("microschemaUuid", "Uuid of the microschema.", MICROSCHEMA_UUID);
		endpoint.method(POST);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Add the microschema to the project.");
		endpoint.exampleResponse(OK, microschemaExamples.getGeolocationMicroschemaResponse(), "Microschema was added to the project.");
		endpoint.events(PROJECT_MICROSCHEMA_ASSIGNED);
		endpoint.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("microschemaUuid");
			crudHandler.handleAddMicroschemaToProject(ac, uuid);
		});
	}

	private void addDeleteHandlers() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:microschemaUuid");
		endpoint.addUriParameter("microschemaUuid", "Uuid of the microschema.", MICROSCHEMA_UUID);
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Remove the microschema from the project.");
		endpoint.exampleResponse(NO_CONTENT, "Microschema was removed from project.");
		endpoint.events(PROJECT_MICROSCHEMA_UNASSIGNED);
		endpoint.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("microschemaUuid");
			crudHandler.handleRemoveMicroschemaFromProject(ac, uuid);
		});
	}
}
