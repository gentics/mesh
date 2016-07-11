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

	private void addDiffHandler() {
		Endpoint diffEndpoint = createEndpoint();
		diffEndpoint.path("/:uuid/diff").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		diffEndpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String schemaUuid = ac.getParameter("uuid");
			crudHandler.handleDiff(ac, schemaUuid);
		});
	}

	private void addChangesHandler() {
		Endpoint readChanges = createEndpoint();
		readChanges.path("/:schemaUuid/changes").method(GET).produces(APPLICATION_JSON);
		readChanges.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String schemaUuid = ac.getParameter("schemaUuid");
			crudHandler.handleGetSchemaChanges(ac, schemaUuid);
		});

		Endpoint executeChanges = createEndpoint();
		executeChanges.path("/:schemaUuid/changes").method(POST).produces(APPLICATION_JSON);
		executeChanges.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String schemaUuid = ac.getParameter("schemaUuid");
			crudHandler.handleApplySchemaChanges(ac, schemaUuid);
		});
	}

	private void addReadHandlers() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
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
		readAll.produces(APPLICATION_JSON);
		readAll.handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	private void addDeleteHandler() {
		Endpoint deleteMicroschema = createEndpoint();
		deleteMicroschema.path("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	private void addUpdateHandler() {
		Endpoint updateMicroschema = createEndpoint();
		updateMicroschema.path("/:uuid").method(PUT).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addCreateHandler() {
		Endpoint createMicroschema = createEndpoint();
		createMicroschema.path("/");
		createMicroschema.method(POST);
		createMicroschema.description("Create a new microschema.");
		createMicroschema.produces(APPLICATION_JSON);
		createMicroschema.handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});

	}

}
