package com.gentics.mesh.core.verticle.schema;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.GET;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractProjectRestVerticle;

/**
 * Verticle for /api/v1/PROJECTNAME/schemas
 */
@Component
@Scope("singleton")
@SpringVerticle
public class ProjectSchemaVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private SchemaContainerCrudHandler crudHandler;

	protected ProjectSchemaVerticle() {
		super("schemas");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addReadHandlers();
	}

	private void addReadHandlers() {
		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleReadProjectList(InternalActionContext.create(rc));
		});
	}
}
