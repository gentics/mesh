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

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.rest.Endpoint;

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

	public SchemaVerticle() {
		super("schemas");
	}

	@Override
	public void registerEndPoints() throws Exception {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/*");
		endpoint.handler(getSpringConfiguration().authHandler());

		addDiffHandler();
		addChangesHandler();

		addCreateHandler();
		addReadHandlers();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addChangesHandler() {
		Endpoint readChanges = createEndpoint();
		readChanges.path("/:schemaUuid/changes").method(GET).produces(APPLICATION_JSON);
		readChanges.handler(rc -> {
			crudHandler.handleGetSchemaChanges(InternalActionContext.create(rc));
		});

		Endpoint executeChanges = createEndpoint();
		executeChanges.path("/:schemaUuid/changes").method(POST).produces(APPLICATION_JSON);
		executeChanges.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String schemaUuid = ac.getParameter("schemaUuid");
			crudHandler.handleApplySchemaChanges(ac, schemaUuid);
		});
	}

	private void addCreateHandler() {
		Endpoint createSchema = createEndpoint();
		createSchema.path("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		createSchema.handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}

	private void addDiffHandler() {
		Endpoint diffEndpoint = createEndpoint();
		diffEndpoint.path("/:uuid/diff");
		diffEndpoint.method(POST);
		diffEndpoint.description("Compare the given schema with the stored schema and create a changeset");
		diffEndpoint.consumes(APPLICATION_JSON);
		diffEndpoint.produces(APPLICATION_JSON);
		diffEndpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleDiff(ac, uuid);
		});
	}

	private void addUpdateHandler() {
		Endpoint updateSchema = createEndpoint();
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addDeleteHandler() {
		Endpoint deleteSchema = createEndpoint();
		deleteSchema.path("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		deleteSchema.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addReadHandlers() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:uuid");
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
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
		readAll.handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});

	}
}
