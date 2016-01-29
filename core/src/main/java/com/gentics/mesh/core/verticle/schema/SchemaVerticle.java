package com.gentics.mesh.core.verticle.schema;

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

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.ext.web.Route;

/**
 * Verticle for /api/v1/schemas endpoint
 */
@Component
@Scope("singleton")
@SpringVerticle
public class SchemaVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private SchemaContainerCrudHandler crudHandler;

	protected SchemaVerticle() {
		super("schemas");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addSchemaProjectHandlers();

		addCreateHandler();
		addReadHandlers();
		addUpdateHandler();
		addDeleteHandler();

		addChangesHandler();
	}

	private void addChangesHandler() {
		Route getRoute = route("/:schemaUuid/changes").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			crudHandler.handleGetSchemaChanges(InternalActionContext.create(rc));
		});

		Route executeRoute = route("/:schemaUuid/changes").method(POST).produces(APPLICATION_JSON);
		executeRoute.handler(rc -> {
			crudHandler.handleExecuteSchemaChanges(InternalActionContext.create(rc));
		});
	}

	private void addSchemaProjectHandlers() {
		Route route = route("/:schemaUuid/projects/:projectUuid").method(PUT).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleAddProjectToSchema(InternalActionContext.create(rc));
		});

		route = route("/:schemaUuid/projects/:projectUuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleRemoveProjectFromSchema(InternalActionContext.create(rc));
		});
	}

	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleUpdate(InternalActionContext.create(rc));
		});
	}

	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleDelete(InternalActionContext.create(rc));
		});
	}

	private void addReadHandlers() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				crudHandler.handleRead(InternalActionContext.create(rc));
			}
		});

		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});

	}
}
