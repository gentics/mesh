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

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractProjectRestVerticle;

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

		// sub handlers
		addChildrenHandler();
		addTagsHandler();
		addMoveHandler();
		addFieldHandlers();
		addLanguageHandlers();
		addNavigationHandlers();

	}

	private void addNavigationHandlers() {
		route("/:uuid/navigation").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleNavigation(ac, uuid);
		});
	}

	private void addLanguageHandlers() {
		route("/:uuid/languages/:languageTag").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			String languageTag = ac.getParameter("languageTag");
			crudHandler.handleDeleteLanguage(ac, uuid, languageTag);
		});
	}

	private void addFieldHandlers() {
		route("/:uuid/languages/:languageTag/fields/:fieldName").method(POST).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			InternalActionContext ac = InternalActionContext.create(rc);
			fieldAPIHandler.handleCreateField(ac, uuid, languageTag, fieldName);
		});

		route("/:uuid/languages/:languageTag/fields/:fieldName").method(GET).handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			fieldAPIHandler.handleReadField(rc, uuid, languageTag, fieldName);
		});

		route("/:uuid/languages/:languageTag/fields/:fieldName").method(PUT).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			InternalActionContext ac = InternalActionContext.create(rc);
			fieldAPIHandler.handleUpdateField(ac, uuid, languageTag, fieldName);
		});

		route("/:uuid/languages/:languageTag/fields/:fieldName").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			String languageTag = ac.getParameter("languageTag");
			String fieldName = ac.getParameter("fieldName");
			fieldAPIHandler.handleRemoveField(ac, uuid, languageTag, fieldName);
		});

		// Image Transformation
		route("/:uuid/languages/:languageTag/fields/:fieldName/transform").method(POST).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			fieldAPIHandler.handleTransformImage(rc, uuid, languageTag, fieldName);
		});

		// List methods
		route("/:uuid/languages/:languageTag/fields/:fieldName/:itemIndex").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			fieldAPIHandler.handleRemoveFieldItem(ac, uuid);
		});

		route("/:uuid/languages/:languageTag/fields/:fieldName/:itemIndex").method(GET).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			fieldAPIHandler.handleReadFieldItem(ac, uuid);
		});

		route("/:uuid/languages/:languageTag/fields/:fieldName/:itemIndex").method(PUT).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			fieldAPIHandler.handleUpdateFieldItem(ac, uuid);
		});

		route("/:uuid/languages/:languageTag/fields/:fieldName/:itemIndex/move/:newItemIndex").method(POST).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			fieldAPIHandler.handleMoveFieldItem(ac, uuid);
		});

		// TODO copy?
		// route("/:uuid/fields/:fieldName/:itemIndex/copy/:newItemIndex").method(POST).handler(rc -> {
		// crudHandler.handleMoveFieldItem(ActionContext.create(rc));
		// });

	}

	private void addMoveHandler() {
		Route route = route("/:uuid/moveTo/:toUuid").method(PUT).produces(APPLICATION_JSON);
		route.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			String toUuid = ac.getParameter("toUuid");
			crudHandler.handleMove(ac, uuid, toUuid);
		});

	}

	private void addChildrenHandler() {
		Route getRoute = route("/:uuid/children").method(GET).produces(APPLICATION_JSON);

		getRoute.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleReadChildren(ac, uuid);
		});
	}

	// TODO filtering, sorting
	private void addTagsHandler() {
		Route getRoute = route("/:uuid/tags").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.readTags(ac, uuid);
		});

		Route postRoute = route("/:uuid/tags/:tagUuid").method(PUT).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			String tagUuid = ac.getParameter("tagUuid");
			crudHandler.handleAddTag(ac, uuid, tagUuid);
		});

		// TODO fix error handling. This does not fail when tagUuid could not be found
		Route deleteRoute = route("/:uuid/tags/:tagUuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			String tagUuid = ac.getParameter("tagUuid");
			crudHandler.handleRemoveTag(ac, uuid, tagUuid);
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
				crudHandler.handleRead(InternalActionContext.create(rc), uuid);
			}
		});

		Route readAllRoute = route("/").method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> crudHandler.handleReadList(InternalActionContext.create(rc)));

	}

	// TODO filter project name
	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleDelete(ac, uuid);
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
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleUpdate(ac, uuid);
		});
	}
}
