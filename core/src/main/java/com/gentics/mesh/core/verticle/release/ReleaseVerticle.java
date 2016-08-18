package com.gentics.mesh.core.verticle.release;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Verticle for REST endpoints to manage Releases
 */
@Component
@Scope("singleton")
@SpringVerticle
public class ReleaseVerticle extends AbstractProjectRestVerticle {
	@Autowired
	private ReleaseCrudHandler crudHandler;

	public ReleaseVerticle() {
		super("releases");
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of releases.";
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
	}

	private void addCreateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new release and automatically invoke a node migration.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(versioningExamples.createReleaseCreateRequest("Winter 2016"));
		endpoint.exampleResponse(201, versioningExamples.createReleaseResponse("Winter 2016"));
		endpoint.handler(rc -> crudHandler.handleCreate(InternalActionContext.create(rc)));
	}

	private void addReadHandler() {
		Endpoint readSchemas = createEndpoint();
		readSchemas.path("/:releaseUuid/schemas");
		readSchemas.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		readSchemas.method(GET);
		readSchemas.description("Load schemas that are assigned to the release and return a paged list response.");
		readSchemas.addQueryParameters(PagingParameters.class);
		readSchemas.produces(APPLICATION_JSON);
		readSchemas.exampleResponse(200, schemaExamples.createSchemaReferenceList());
		readSchemas.handler(rc -> {
			String uuid = rc.request().getParam("releaseUuid");
			crudHandler.handleGetSchemaVersions(InternalActionContext.create(rc), uuid);
		});

		Endpoint readMicroschemas = createEndpoint();
		readMicroschemas.path("/:releaseUuid/microschemas");
		readMicroschemas.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		readMicroschemas.method(GET);
		readMicroschemas.description("Load schemas that are assigned to the release and return a paged list response.");
		readMicroschemas.produces(APPLICATION_JSON);
		readMicroschemas.exampleResponse(200, microschemaExamples.createMicroschemaReferenceList());
		readMicroschemas.addQueryParameters(PagingParameters.class);
		readMicroschemas.handler(rc -> {
			String uuid = rc.request().getParam("releaseUuid");
			crudHandler.handleGetMicroschemaVersions(InternalActionContext.create(rc), uuid);
		});

		Endpoint readOne = createEndpoint();
		readOne.path("/:releaseUuid");
		readOne.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		readOne.description("Load the release with the given uuid.");
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(200, versioningExamples.createReleaseResponse("Summer Collection Release"));
		readOne.handler(rc -> {
			String uuid = rc.request().params().get("releaseUuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				crudHandler.handleRead(InternalActionContext.create(rc), uuid);
			}
		});

		Endpoint readAll = createEndpoint();
		readAll.path("/");
		readAll.method(GET);
		readAll.description("Load multiple releases and return a paged list response.");
		readAll.exampleResponse(200, versioningExamples.createReleaseListResponse());
		readAll.addQueryParameters(PagingParameters.class);
		readAll.produces(APPLICATION_JSON);
		readAll.handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	private void addUpdateHandler() {
		Endpoint addSchema = createEndpoint();
		addSchema.path("/:releaseUuid/schemas");
		addSchema.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		addSchema.method(POST);
		addSchema.description("Assign a schema version to the release.");
		addSchema.consumes(APPLICATION_JSON);
		addSchema.produces(APPLICATION_JSON);
		addSchema.exampleRequest(schemaExamples.createSchemaReferenceList());
		addSchema.exampleResponse(200, schemaExamples.createSchemaReferenceList());
		addSchema.handler(rc -> {
			String uuid = rc.request().params().get("releaseUuid");
			crudHandler.handleAssignSchemaVersion(InternalActionContext.create(rc), uuid);
		});

		Endpoint addMicroschema = createEndpoint();
		addMicroschema.path("/:releaseUuid/microschemas");
		addMicroschema.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		addMicroschema.method(POST);
		addMicroschema.description("Assign a microschema version to the release.");
		addMicroschema.consumes(APPLICATION_JSON);
		addMicroschema.produces(APPLICATION_JSON);
		addMicroschema.exampleRequest(microschemaExamples.createMicroschemaReferenceList());
		addMicroschema.exampleResponse(200, microschemaExamples.createMicroschemaReferenceList());
		addMicroschema.handler(rc -> {
			String uuid = rc.request().params().get("releaseUuid");
			crudHandler.handleAssignMicroschemaVersion(InternalActionContext.create(rc), uuid);
		});

		Endpoint updateRelease = createEndpoint();
		updateRelease.path("/:releaseUuid");
		updateRelease.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		updateRelease.description("Update the release with the given uuid.");
		updateRelease.method(POST);
		updateRelease.consumes(APPLICATION_JSON);
		updateRelease.produces(APPLICATION_JSON);
		updateRelease.exampleRequest(versioningExamples.createReleaseUpdateRequest("Winter Collection Release"));
		updateRelease.exampleResponse(200, versioningExamples.createReleaseResponse("Winter Collection Release"));
		updateRelease.handler(rc -> {
			String uuid = rc.request().params().get("releaseUuid");
			crudHandler.handleUpdate(InternalActionContext.create(rc), uuid);
		});
	}
}
