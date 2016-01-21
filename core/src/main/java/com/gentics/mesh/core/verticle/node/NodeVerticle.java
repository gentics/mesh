package com.gentics.mesh.core.verticle.node;

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

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.ext.web.Route;

/**
 * The content verticle adds rest endpoints for manipulating nodes.
 */
@Component
@Scope("singleton")
@SpringVerticle
public class NodeVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private NodeCrudHandler crudHandler;

	@Autowired
	private NodeFieldAPIHandler fieldAPIHandler;

	public NodeVerticle() {
		super("nodes");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		route("/:uuid").handler(crudHandler.getUuidHandler("node_not_found_for_uuid"));

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
		addChildrenHandler();
		addTagsHandler();
		addMoveHandler();
		addFieldHandlers();

		addLanguageHandlers();

	}

	private void addLanguageHandlers() {
		route("/:uuid/languages/:languageTag").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleDeleteLanguage(InternalActionContext.create(rc));
		});

	}

	private void addFieldHandlers() {
		route("/:uuid/languages/:languageTag/fields/:fieldName").method(POST).produces(APPLICATION_JSON).handler(rc -> {
			fieldAPIHandler.handleCreateField(rc);
		});

		route("/:uuid/languages/:languageTag/fields/:fieldName").method(GET).handler(rc -> {
			fieldAPIHandler.handleReadField(rc);
		});

		route("/:uuid/languages/:languageTag/fields/:fieldName").method(PUT).produces(APPLICATION_JSON).handler(rc -> {
			fieldAPIHandler.handleUpdateField(rc);
		});

		route("/:uuid/languages/:languageTag/fields/:fieldName").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			fieldAPIHandler.handleRemoveField(InternalActionContext.create(rc));
		});

		// Image Transformation
		route("/:uuid/languages/:languageTag/fields/:fieldName/transform").method(POST).produces(APPLICATION_JSON).handler(rc -> {
			fieldAPIHandler.handleTransformImage(rc);
		});

		// List methods
		route("/:uuid/languages/:languageTag/fields/:fieldName/:itemIndex").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			fieldAPIHandler.handleRemoveFieldItem(InternalActionContext.create(rc));
		});

		route("/:uuid/languages/:languageTag/fields/:fieldName/:itemIndex").method(GET).handler(rc -> {
			fieldAPIHandler.handleReadFieldItem(InternalActionContext.create(rc));
		});

		route("/:uuid/languages/:languageTag/fields/:fieldName/:itemIndex").method(PUT).handler(rc -> {
			fieldAPIHandler.handleUpdateFieldItem(InternalActionContext.create(rc));
		});

		route("/:uuid/languages/:languageTag/fields/:fieldName/:itemIndex/move/:newItemIndex").method(POST).handler(rc -> {
			fieldAPIHandler.handleMoveFieldItem(InternalActionContext.create(rc));
		});

		// TODO copy?
		// route("/:uuid/fields/:fieldName/:itemIndex/copy/:newItemIndex").method(POST).handler(rc -> {
		// crudHandler.handleMoveFieldItem(ActionContext.create(rc));
		// });

	}

	private void addMoveHandler() {
		Route route = route("/:uuid/moveTo/:toUuid").method(PUT).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleMove(InternalActionContext.create(rc));
		});

	}

	private void addChildrenHandler() {
		Route getRoute = route("/:uuid/children").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			crudHandler.handleReadChildren(InternalActionContext.create(rc));
		});
	}

	// TODO filtering, sorting
	private void addTagsHandler() {
		Route getRoute = route("/:uuid/tags").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			crudHandler.readTags(InternalActionContext.create(rc));
		});

		Route postRoute = route("/:uuid/tags/:tagUuid").method(PUT).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			crudHandler.handleAddTag(InternalActionContext.create(rc));
		});

		// TODO fix error handling. This does not fail when tagUuid could not be found
		Route deleteRoute = route("/:uuid/tags/:tagUuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			crudHandler.handleRemoveTag(InternalActionContext.create(rc));
		});
	}

	// TODO handle schema by name / by uuid - move that code in a separate
	// handler
	private void addCreateHandler() {
		Route route = route("/").method(POST).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}

	// TODO filter by project name
	// TODO filtering
	private void addReadHandler() {

		Route route = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			// TODO move if clause back into verticle
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				crudHandler.handleRead(InternalActionContext.create(rc));
			}
		});

		Route readAllRoute = route("/").method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});

	}

	// TODO filter project name
	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleDelete(InternalActionContext.create(rc));
		});
	}

	// TODO filter by project name
	// TODO handle depth
	// TODO update other fields as well?
	// TODO Update user information
	// TODO use schema and only handle those i18n properties that were specified
	// within the schema.
	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleUpdate(InternalActionContext.create(rc));
		});
	}
}
