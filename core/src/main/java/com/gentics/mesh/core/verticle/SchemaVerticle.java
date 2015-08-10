package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.web.Route;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.verticle.handler.SchemaContainerCRUDHandler;
@Component
@Scope("singleton")
@SpringVerticle
public class SchemaVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private SchemaContainerCRUDHandler crudHandler;

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
	}

	private void addSchemaProjectHandlers() {
		Route route = route("/:schemaUuid/projects/:projectUuid").method(POST).produces(APPLICATION_JSON);
		route.handler(rc -> {
			loadObject(rc, "projectUuid", UPDATE_PERM, boot.projectRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					loadObject(rc, "schemaUuid", READ_PERM, boot.schemaContainerRoot(), srh -> {
						if (hasSucceeded(rc, srh)) {
							Project project = rh.result();
							SchemaContainer schema = srh.result();
							project.getSchemaContainerRoot().addSchemaContainer(schema);

							// TODO add simple message or return schema?
							transformAndResponde(rc, schema);
						}
					});
				}
			});

		});

		route = route("/:schemaUuid/projects/:projectUuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			loadObject(rc, "projectUuid", UPDATE_PERM, boot.projectRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					// TODO check whether schema is assigned to project
					loadObject(rc, "schemaUuid", READ_PERM, boot.schemaContainerRoot(), srh -> {
						if (hasSucceeded(rc, srh)) {
							SchemaContainer schema = srh.result();
							Project project = rh.result();
							project.getSchemaContainerRoot().removeSchemaContainer(schema);
							transformAndResponde(rc, schema);
						}
					});
				}
			});
		});
	}

	// TODO set creator
	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleCreate(rc);
		});
	}

	// TODO update modification timestamps

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleUpdate(rc);
		});
	}

	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleDelete(rc);
		});
	}

	private void addReadHandlers() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleRead(rc);
		});

		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleReadList(rc);
		});

	}
}
