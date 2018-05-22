package com.gentics.mesh.core.endpoint.release;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.auth.MeshAuthHandler;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Verticle for REST endpoints to manage Releases.
 */
public class ReleaseEndpoint extends AbstractProjectEndpoint {

	private ReleaseCrudHandler crudHandler;

	public ReleaseEndpoint() {
		super("releases", null, null);
	}

	@Inject
	public ReleaseEndpoint(MeshAuthHandler handler, BootstrapInitializer boot, ReleaseCrudHandler crudHandler) {
		super("releases", handler, boot);
		this.crudHandler = crudHandler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of releases.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addCreateHandler();
		addSchemaInfoHandler();
		addMicroschemaInfoHandler();
		addReadHandler();
		addUpdateHandler();
		addNodeMigrationHandler();
		addMicronodeMigrationHandler();
	}

	private void addMicroschemaInfoHandler() {
		InternalEndpointRoute readMicroschemas = createRoute();
		readMicroschemas.path("/:releaseUuid/microschemas");
		readMicroschemas.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		readMicroschemas.method(GET);
		readMicroschemas.description("Load microschemas that are assigned to the release and return a paged list response.");
		readMicroschemas.produces(APPLICATION_JSON);
		readMicroschemas.exampleResponse(OK, microschemaExamples.createMicroschemaReferenceList(), "List of microschemas.");
		readMicroschemas.addQueryParameters(PagingParametersImpl.class);
		readMicroschemas.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam("releaseUuid");
			crudHandler.handleGetMicroschemaVersions(ac, uuid);
		});

		
	}

	private void addSchemaInfoHandler() {
		InternalEndpointRoute readSchemas = createRoute();
		readSchemas.path("/:releaseUuid/schemas");
		readSchemas.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		readSchemas.method(GET);
		readSchemas.description("Load schemas that are assigned to the release and return a paged list response.");
		readSchemas.addQueryParameters(PagingParametersImpl.class);
		readSchemas.produces(APPLICATION_JSON);
		readSchemas.exampleResponse(OK, releaseExamples.createSchemaReferenceList(), "Loaded schema list.");
		readSchemas.handler(rc -> {
			String uuid = rc.request().getParam("releaseUuid");
			InternalActionContext ac = wrap(rc);
			crudHandler.handleGetSchemaVersions(ac, uuid);
		});
	}

	private void addNodeMigrationHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:releaseUuid/migrateSchemas");
		endpoint.method(POST);
		endpoint.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		endpoint.description("Invoked the node migration for not yet migrated nodes of schemas that are assigned to the release.");
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "schema_migration_invoked");
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String releaseUuid = rc.request().getParam("releaseUuid");
			crudHandler.handleMigrateRemainingNodes(ac, releaseUuid);
		});
	}

	private void addMicronodeMigrationHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:releaseUuid/migrateMicroschemas");
		endpoint.method(POST);
		endpoint.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		endpoint.description("Invoked the micronode migration for not yet migrated micronodes of microschemas that are assigned to the release.");
		endpoint.exampleResponse(OK, miscExamples.createMessageResponse(), "schema_migration_invoked");
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String releaseUuid = rc.request().getParam("releaseUuid");
			crudHandler.handleMigrateRemainingMicronodes(ac, releaseUuid);
		});
	}

	private void addCreateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new release and automatically invoke a node migration.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(versioningExamples.createReleaseCreateRequest("Winter 2016"));
		endpoint.exampleResponse(CREATED, versioningExamples.createReleaseResponse("Winter 2016"), "Created release.");
		endpoint.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleCreate(ac);
		});
	}

	private void addReadHandler() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:releaseUuid");
		readOne.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		readOne.description("Load the release with the given uuid.");
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, versioningExamples.createReleaseResponse("Summer Collection Release"), "Loaded release.");
		readOne.handler(rc -> {
			String uuid = rc.request().params().get("releaseUuid");
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
		readAll.description("Load multiple releases and return a paged list response.");
		readAll.exampleResponse(OK, versioningExamples.createReleaseListResponse(), "Loaded releases.");
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.produces(APPLICATION_JSON);
		readAll.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadList(ac);
		});
	}

	private void addUpdateHandler() {
		InternalEndpointRoute addSchema = createRoute();
		addSchema.path("/:releaseUuid/schemas");
		addSchema.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		addSchema.method(POST);
		addSchema.description("Assign a schema version to the release.");
		addSchema.consumes(APPLICATION_JSON);
		addSchema.produces(APPLICATION_JSON);
		addSchema.exampleRequest(releaseExamples.createSchemaReferenceList());
		addSchema.exampleResponse(OK, releaseExamples.createSchemaReferenceList(), "Updated schema list.");
		addSchema.handler(rc -> {
			String uuid = rc.request().params().get("releaseUuid");
			InternalActionContext ac = wrap(rc);
			crudHandler.handleAssignSchemaVersion(ac, uuid);
		});

		InternalEndpointRoute addMicroschema = createRoute();
		addMicroschema.path("/:releaseUuid/microschemas");
		addMicroschema.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		addMicroschema.method(POST);
		addMicroschema.description("Assign a microschema version to the release.");
		addMicroschema.consumes(APPLICATION_JSON);
		addMicroschema.produces(APPLICATION_JSON);
		addMicroschema.exampleRequest(microschemaExamples.createMicroschemaReferenceList());
		addMicroschema.exampleResponse(OK, microschemaExamples.createMicroschemaReferenceList(), "Updated microschema list.");
		addMicroschema.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().params().get("releaseUuid");
			crudHandler.handleAssignMicroschemaVersion(ac, uuid);
		});

		InternalEndpointRoute updateRelease = createRoute();
		updateRelease.path("/:releaseUuid");
		updateRelease.addUriParameter("releaseUuid", "Uuid of the release", UUIDUtil.randomUUID());
		updateRelease
				.description("Update the release with the given uuid. The release is created if no release with the specified uuid could be found.");
		updateRelease.method(POST);
		updateRelease.consumes(APPLICATION_JSON);
		updateRelease.produces(APPLICATION_JSON);
		updateRelease.exampleRequest(versioningExamples.createReleaseUpdateRequest("Winter Collection Release"));
		updateRelease.exampleResponse(OK, versioningExamples.createReleaseResponse("Winter Collection Release"), "Updated release");
		updateRelease.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().params().get("releaseUuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}
}
