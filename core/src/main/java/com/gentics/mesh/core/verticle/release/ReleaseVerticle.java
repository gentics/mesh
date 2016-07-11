package com.gentics.mesh.core.verticle.release;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.rest.Endpoint;

import io.vertx.ext.web.Route;

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
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
	}

	private void addCreateHandler() {
		Route route = route("/").method(POST).produces(APPLICATION_JSON);
		route.handler(rc -> crudHandler.handleCreate(InternalActionContext.create(rc)));
	}

	private void addReadHandler() {
		Endpoint readSchemas = createEndpoint();
		readSchemas.path("/:uuid/schemas").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			crudHandler.handleGetSchemaVersions(InternalActionContext.create(rc), uuid);
		});

		Endpoint readMicroschemas = createEndpoint();
		readMicroschemas.path("/:uuid/microschemas").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			crudHandler.handleGetMicroschemaVersions(InternalActionContext.create(rc), uuid);
		});

		Endpoint readOne = createEndpoint();
		readOne.path("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				crudHandler.handleRead(InternalActionContext.create(rc), uuid);
			}
		});

		Endpoint readAll = createEndpoint();
		readAll.path("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	private void addUpdateHandler() {
		Endpoint addSchema = createEndpoint();
		addSchema.path("/:uuid/schemas");
		addSchema.method(PUT);
		addSchema.consumes(APPLICATION_JSON);
		addSchema.produces(APPLICATION_JSON);
		addSchema.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			crudHandler.handleAssignSchemaVersion(InternalActionContext.create(rc), uuid);
		});

		Endpoint addMicroschema = createEndpoint();
		addMicroschema.path("/:uuid/microschemas");
		addMicroschema.method(PUT);
		addMicroschema.consumes(APPLICATION_JSON);
		addMicroschema.produces(APPLICATION_JSON);
		addMicroschema.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			crudHandler.handleAssignMicroschemaVersion(InternalActionContext.create(rc), uuid);
		});

		Endpoint updateRelease = createEndpoint();
		updateRelease.path("/:uuid");
		updateRelease.method(PUT);
		updateRelease.consumes(APPLICATION_JSON);
		updateRelease.produces(APPLICATION_JSON);
		updateRelease.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			crudHandler.handleUpdate(InternalActionContext.create(rc), uuid);
		});
	}
}
