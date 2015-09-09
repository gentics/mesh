package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.core.HttpConstants.APPLICATION_JSON;
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
import com.gentics.mesh.handler.ActionContext;

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

	public NodeVerticle() {
		super("nodes");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
		addChildrenHandler();
		addTagsHandler();
		addMoveHandler();
		addFieldHandlers();

		addFileuploadHandler();
		addFileDownloadHandler();
	}

	private void addFieldHandlers() {
		route("/:uuid/fields/:fieldName").method(GET).handler(rc -> {
			crudHandler.handleReadField(ActionContext.create(rc));
		});

		route("/:uuid/fields/:fieldName").method(PUT).handler(rc -> {
			crudHandler.handleUpdateField(ActionContext.create(rc));
		});

		route("/:uuid/fields/:fieldName").method(DELETE).handler(rc -> {
			crudHandler.handleRemoveField(ActionContext.create(rc));
		});

		// List methods

		route("/:uuid/fields/:fieldName").method(POST).handler(rc -> {
			crudHandler.handleAddFieldItem(ActionContext.create(rc));
		});

		route("/:uuid/fields/:fieldName/:itemIndex").method(DELETE).handler(rc -> {
			crudHandler.handleRemoveFieldItem(ActionContext.create(rc));
		});

		route("/:uuid/fields/:fieldName/:itemIndex").method(GET).handler(rc -> {
			crudHandler.handleReadFieldItem(ActionContext.create(rc));
		});

		route("/:uuid/fields/:fieldName/:itemIndex").method(PUT).handler(rc -> {
			crudHandler.handleUpdateFieldItem(ActionContext.create(rc));
		});

		route("/:uuid/fields/:fieldName/:itemIndex/move/:newItemIndex").method(POST).handler(rc -> {
			crudHandler.handleMoveFieldItem(ActionContext.create(rc));
		});

		//TODO copy?
		//		route("/:uuid/fields/:fieldName/:itemIndex/copy/:newItemIndex").method(POST).handler(rc -> {
		//			crudHandler.handleMoveFieldItem(ActionContext.create(rc));
		//		});

	}

	private void addFileDownloadHandler() {
		route("/:uuid/bin").method(GET).handler(rc -> {
			crudHandler.handleDownload(rc);
		});
	}

	private void addFileuploadHandler() {
		route("/:uuid/bin").method(POST).method(PUT).handler(rc -> {
			crudHandler.handleUpload(rc);
		});
	}

	private void addMoveHandler() {
		Route route = route("/:uuid/moveTo/:toUuid").method(PUT).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleMove(ActionContext.create(rc));
		});

	}

	private void addChildrenHandler() {
		Route getRoute = route("/:uuid/children").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			crudHandler.handleReadChildren(ActionContext.create(rc));
		});
	}

	// TODO filtering, sorting
	private void addTagsHandler() {
		Route getRoute = route("/:uuid/tags").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			crudHandler.readTags(ActionContext.create(rc));
		});

		Route postRoute = route("/:uuid/tags/:tagUuid").method(POST).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			crudHandler.handleAddTag(ActionContext.create(rc));
		});

		// TODO fix error handling. This does not fail when tagUuid could not be found
		Route deleteRoute = route("/:uuid/tags/:tagUuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			crudHandler.handleRemoveTag(ActionContext.create(rc));
		});
	}

	// TODO handle schema by name / by uuid - move that code in a separate
	// handler
	private void addCreateHandler() {
		Route route = route("/").method(POST).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleCreate(ActionContext.create(rc));
		});
	}

	// TODO filter by project name
	// TODO filtering
	private void addReadHandler() {

		Route route = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			//TODO move if back into verticle 
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				crudHandler.handleRead(ActionContext.create(rc));
			}
		});

		Route readAllRoute = route("/").method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			crudHandler.handleReadList(ActionContext.create(rc));
		});

	}

	// TODO filter project name
	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleDelete(ActionContext.create(rc));
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
			crudHandler.handleUpdate(ActionContext.create(rc));
		});
	}
}
