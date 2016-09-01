package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.Endpoint;

import dagger.Lazy;

/**
 * Verticle for /api/v1/PROJECTNAME/schemas
 */
@Singleton
public class ProjectSchemaVerticle extends AbstractProjectRestVerticle {

	private SchemaContainerCrudHandler crudHandler;

	@Inject
	public ProjectSchemaVerticle(Lazy<BootstrapInitializer> boot, RouterStorage routerStorage, SchemaContainerCrudHandler crudHandler) {
		super("schemas", boot, routerStorage);
		this.crudHandler = crudHandler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which can be used to assign schemas to projects.";
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();
		addReadHandlers();
		addUpdateHandlers();
		addDeleteHandlers();
	}

	private void addReadHandlers() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			crudHandler.handleReadProjectList(InternalActionContext.create(rc));
		});
	}

	private void addUpdateHandlers() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid");
		endpoint.method(POST);
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleAddSchemaToProject(ac, uuid);
		});
	}

	private void addDeleteHandlers() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid");
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleRemoveSchemaFromProject(ac, uuid);
		});
	}
}
