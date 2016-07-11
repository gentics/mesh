package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.raml.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.navigation.NavigationElement;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.util.UUIDUtil;

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

	private Resource resource = new Resource();

	@Autowired
	private NodeFieldAPIHandler fieldAPIHandler;

	public NodeVerticle() {
		super("nodes");

	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(getSpringConfiguration().authHandler());
		if (getCrudHandler() != null) {
			route("/:uuid").handler(getCrudHandler().getUuidHandler("node_not_found_for_uuid"));
		}

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
		addPublishHandlers();
	}

	public Resource getResource() {
		return resource;
	}

	private void addNavigationHandlers() {

		NavigationResponse responseExample = new NavigationResponse();
		NavigationElement root = new NavigationElement();
		root.setUuid(UUIDUtil.randomUUID());
		responseExample.setRoot(root);

		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid/navigation");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Returns a navigation object for the provided node.");
		endpoint.displayName("Navigation");
		endpoint.exampleRequest(new GenericMessageResponse("test"));
		endpoint.exampleResponse(200, responseExample);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleNavigation(ac, uuid);
		});
	}

	private void addLanguageHandlers() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid/languages/:languageTag");
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			String languageTag = ac.getParameter("languageTag");
			crudHandler.handleDeleteLanguage(ac, uuid, languageTag);
		});
	}

	private void addFieldHandlers() {
		Endpoint fieldUpdate = createEndpoint();
		fieldUpdate.path("/:uuid/languages/:languageTag/fields/:fieldName");
		fieldUpdate.method(POST);
		fieldUpdate.produces(APPLICATION_JSON);
		fieldUpdate.traits("paged");
		fieldUpdate.handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			InternalActionContext ac = InternalActionContext.create(rc);
			fieldAPIHandler.handleCreateField(ac, uuid, languageTag, fieldName);
		});

		Endpoint fieldGet = createEndpoint();
		fieldGet.path("/:uuid/languages/:languageTag/fields/:fieldName");
		fieldGet.method(GET);
		fieldGet.handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			fieldAPIHandler.handleReadField(rc, uuid, languageTag, fieldName);
		});

		Endpoint fieldCreate = createEndpoint();
		fieldCreate.path("/:uuid/languages/:languageTag/fields/:fieldName");
		fieldCreate.method(PUT).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			InternalActionContext ac = InternalActionContext.create(rc);
			fieldAPIHandler.handleUpdateField(ac, uuid, languageTag, fieldName);
		});

		Endpoint fieldDelete = createEndpoint();
		fieldDelete.path("/:uuid/languages/:languageTag/fields/:fieldName");
		fieldDelete.method(DELETE);
		fieldDelete.produces(APPLICATION_JSON);
		fieldDelete.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			String languageTag = ac.getParameter("languageTag");
			String fieldName = ac.getParameter("fieldName");
			fieldAPIHandler.handleRemoveField(ac, uuid, languageTag, fieldName);
		});

		// Image Transformation
		Endpoint imageTransform = createEndpoint();
		imageTransform.path("/:uuid/languages/:languageTag/fields/:fieldName/transform");
		imageTransform.method(POST);
		imageTransform.produces(APPLICATION_JSON);
		imageTransform.handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			fieldAPIHandler.handleTransformImage(rc, uuid, languageTag, fieldName);
		});

		// List methods
		Endpoint listItemDelete = createEndpoint();
		listItemDelete.path("/:uuid/languages/:languageTag/fields/:fieldName/:itemIndex");
		listItemDelete.method(DELETE);
		listItemDelete.produces(APPLICATION_JSON);
		listItemDelete.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			fieldAPIHandler.handleRemoveFieldItem(ac, uuid);
		});

		Endpoint listItemGet = createEndpoint();
		listItemGet.path("/:uuid/languages/:languageTag/fields/:fieldName/:itemIndex");
		listItemGet.method(GET);
		listItemGet.handler(rc -> {
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
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid/moveTo/:toUuid");
		endpoint.method(PUT);
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			String toUuid = ac.getParameter("toUuid");
			crudHandler.handleMove(ac, uuid, toUuid);
		});
	}

	private void addChildrenHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid/children");
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
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
		Endpoint endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new node.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			ac.getVersioningParameters().setVersion("draft");
			crudHandler.handleCreate(ac);
		});
	}

	// TODO filter by project name
	// TODO filtering
	private void addReadHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid");
		endpoint.method(GET);
		endpoint.description("Load the node with the given uuid.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			// TODO move if clause back into verticle
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				crudHandler.handleRead(InternalActionContext.create(rc), uuid);
			}
		});

		Endpoint readAllEndpoint = createEndpoint();
		readAllEndpoint.path("/");
		readAllEndpoint.description("Read all nodes and return a paged list response.");
		readAllEndpoint.method(GET);
		readAllEndpoint.produces(APPLICATION_JSON);
		readAllEndpoint.handler(rc -> crudHandler.handleReadList(InternalActionContext.create(rc)));

	}

	// TODO filter project name
	private void addDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid");
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Delete the node with the given uuid.");
		endpoint.handler(rc -> {
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
		Endpoint endpoint = createEndpoint();
		endpoint.description("Update the node with the given uuid.");
		endpoint.path("/:uuid");
		endpoint.method(PUT);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			ac.getVersioningParameters().setVersion("draft");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addPublishHandlers() {

		Endpoint getEndpoint = createEndpoint();
		getEndpoint.description("Return the published status of the node.");
		getEndpoint.path("/:uuid/published");
		getEndpoint.method(GET);
		getEndpoint.produces(APPLICATION_JSON);
		getEndpoint.handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			crudHandler.handleGetPublishStatus(InternalActionContext.create(rc), uuid);
		});

		Endpoint putEndpoint = createEndpoint();
		putEndpoint.description("Publish the node with the given uuid.");
		putEndpoint.path("/:uuid/published");
		putEndpoint.method(PUT);
		putEndpoint.produces(APPLICATION_JSON);
		putEndpoint.handler(rc -> {
			crudHandler.handlePublish(InternalActionContext.create(rc), rc.request().getParam("uuid"));
		});

		Endpoint deleteEndpoint = createEndpoint();
		deleteEndpoint.description("Unpublish the given node.");
		deleteEndpoint.path("/:uuid/published");
		deleteEndpoint.method(DELETE);
		deleteEndpoint.produces(APPLICATION_JSON);
		deleteEndpoint.handler(rc -> {
			crudHandler.handleTakeOffline(InternalActionContext.create(rc), rc.request().getParam("uuid"));
		});

		Endpoint getLanguageRoute = createEndpoint();
		getLanguageRoute.description("Return the published status for the given language of the node.");
		getLanguageRoute.path("/:uuid/languages/:languageTag/published");
		getLanguageRoute.method(GET);
		getLanguageRoute.produces(APPLICATION_JSON);
		getLanguageRoute.handler(rc -> {
			crudHandler.handleGetPublishStatus(InternalActionContext.create(rc), rc.request().getParam("uuid"), rc.request().getParam("languageTag"));
		});

		Endpoint putLanguageRoute = createEndpoint();
		putLanguageRoute.path("/:uuid/languages/:languageTag/published").method(PUT).produces(APPLICATION_JSON);
		putLanguageRoute.description("Publish the language of the node.");
		putLanguageRoute.handler(rc -> {
			crudHandler.handlePublish(InternalActionContext.create(rc), rc.request().getParam("uuid"), rc.request().getParam("languageTag"));
		});

		Endpoint deleteLanguageRoute = createEndpoint();
		deleteEndpoint.description("Take the language of the node offline.");
		deleteLanguageRoute.path("/:uuid/languages/:languageTag/published").method(DELETE).produces(APPLICATION_JSON);
		deleteLanguageRoute.handler(rc -> {
			crudHandler.handleTakeOffline(InternalActionContext.create(rc), rc.request().getParam("uuid"), rc.request().getParam("languageTag"));
		});

	}

	public NodeCrudHandler getCrudHandler() {
		return crudHandler;
	}
}
