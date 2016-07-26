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
import com.gentics.mesh.parameter.impl.NavigationParameters;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.PublishParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.util.UUIDUtil;

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
		secureAll();
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
		endpoint.addQueryParameters(NavigationParameters.class);
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
		endpoint.description("Delete the language specific content of the node.");
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
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
		// TODO consumes json and upload
		fieldUpdate.exampleResponse(200, miscExamples.getMessageResponse());
		fieldUpdate.description("Update the field with the given name.");
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
		fieldGet.description("Load the field with the given name.");
		fieldGet.handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			fieldAPIHandler.handleReadField(rc, uuid, languageTag, fieldName);
		});

		Endpoint fieldCreate = createEndpoint();
		fieldCreate.path("/:uuid/languages/:languageTag/fields/:fieldName");
		fieldCreate.description("Create a new field with the given name.");
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
		fieldDelete.description("Delete the field with the given name");
		fieldDelete.exampleResponse(200, miscExamples.getMessageResponse());
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
		imageTransform.consumes(APPLICATION_JSON);
		imageTransform.description("Transform the image with the given field name and overwrite the stored image with the transformation result.");
		imageTransform.exampleRequest(nodeExamples.getBinaryFieldTransformRequest());
		imageTransform.exampleResponse(200, miscExamples.getMessageResponse());
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
		listItemDelete.exampleResponse(200, miscExamples.getMessageResponse());
		listItemDelete.description("Delete the field list item at the given index position (Not yet implemented)");
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
		listItemGet.exampleResponse(200, miscExamples.getMessageResponse());
		listItemGet.description("Load the field list item at the given index position. (Not yet implemented)");
		listItemGet.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			fieldAPIHandler.handleReadFieldItem(ac, uuid);
		});

		Endpoint updateFieldItem = createEndpoint();
		updateFieldItem.path("/:uuid/languages/:languageTag/fields/:fieldName/:itemIndex");
		updateFieldItem.method(PUT);
		updateFieldItem.description("Update the field list item at the given index position. (Not yet implemented)");
		updateFieldItem.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = rc.request().getParam("uuid");
			String languageTag = rc.request().getParam("languageTag");
			String fieldName = rc.request().getParam("fieldName");
			fieldAPIHandler.handleUpdateFieldItem(ac, uuid);
		});

		Endpoint moveFieldItem = createEndpoint();
		moveFieldItem.path("/:uuid/languages/:languageTag/fields/:fieldName/:itemIndex/move/:newItemIndex");
		moveFieldItem.method(POST);
		moveFieldItem.description("Move the field list item on a new index position. (Not yet implemented)");
		moveFieldItem.handler(rc -> {
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
		endpoint.description("Move the node into the target node.");
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
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
		endpoint.exampleResponse(200, nodeExamples.getNodeListResponse());
		endpoint.description("Load all child nodes and return a paged list response.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleReadChildren(ac, uuid);
		});
	}

	// TODO filtering, sorting
	private void addTagsHandler() {
		Endpoint getTags = createEndpoint();
		getTags.path("/:uuid/tags");
		getTags.method(GET);
		getTags.produces(APPLICATION_JSON);
		getTags.exampleResponse(200, tagExamples.getTagListResponse());
		getTags.description("Return a list of all tags which tag the node.");
		getTags.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.readTags(ac, uuid);
		});

		Endpoint addTag = createEndpoint();
		addTag.path("/:uuid/tags/:tagUuid");
		addTag.method(PUT);
		addTag.produces(APPLICATION_JSON);
		addTag.exampleResponse(200, nodeExamples.getNodeResponse2());
		addTag.description("Assign the given tag to the node.");
		addTag.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			String tagUuid = ac.getParameter("tagUuid");
			crudHandler.handleAddTag(ac, uuid, tagUuid);
		});

		// TODO fix error handling. This does not fail when tagUuid could not be found
		Endpoint removeTag = createEndpoint();
		removeTag.path("/:uuid/tags/:tagUuid");
		removeTag.method(DELETE);
		removeTag.produces(APPLICATION_JSON);
		removeTag.description("Remove the given tag from the node.");
		removeTag.exampleResponse(200, nodeExamples.getNodeResponse2());
		removeTag.handler(rc -> {
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
		endpoint.exampleRequest(nodeExamples.getNodeCreateRequest());
		endpoint.exampleResponse(201, nodeExamples.getNodeResponseWithAllFields());
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			ac.getVersioningParameters().setVersion("draft");
			crudHandler.handleCreate(ac);
		});
	}

	// TODO filter by project name
	// TODO filtering
	private void addReadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:uuid");
		readOne.method(GET);
		readOne.description("Load the node with the given uuid.");
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(200, nodeExamples.getNodeResponseWithAllFields());
		readOne.addQueryParameters(VersioningParameters.class);
		readOne.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			// TODO move if clause back into verticle
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				crudHandler.handleRead(InternalActionContext.create(rc), uuid);
			}
		});

		Endpoint readAll = createEndpoint();
		readAll.path("/");
		readAll.description("Read all nodes and return a paged list response.");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
		readAll.exampleResponse(200, nodeExamples.getNodeListResponse());
		readAll.addQueryParameters(VersioningParameters.class);
		readAll.addQueryParameters(NodeParameters.class);
		readAll.addQueryParameters(PagingParameters.class);
		readAll.handler(rc -> crudHandler.handleReadList(InternalActionContext.create(rc)));

	}

	// TODO filter project name
	private void addDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid");
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Delete the node with the given uuid.");
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
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
		endpoint.exampleRequest(nodeExamples.getNodeUpdateRequest());
		endpoint.exampleResponse(200, nodeExamples.getNodeResponse2());
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
		getEndpoint.exampleResponse(200, versioningExamples.getPublishStatusResponse());
		getEndpoint.handler(rc -> {
			String uuid = rc.request().getParam("uuid");
			crudHandler.handleGetPublishStatus(InternalActionContext.create(rc), uuid);
		});

		Endpoint putEndpoint = createEndpoint();
		putEndpoint.description("Publish the node with the given uuid.");
		putEndpoint.path("/:uuid/published");
		putEndpoint.method(PUT);
		putEndpoint.produces(APPLICATION_JSON);
		putEndpoint.exampleResponse(200, versioningExamples.getPublishStatusResponse());
		putEndpoint.addQueryParameters(PublishParameters.class);
		putEndpoint.handler(rc -> {
			crudHandler.handlePublish(InternalActionContext.create(rc), rc.request().getParam("uuid"));
		});

		Endpoint deleteEndpoint = createEndpoint();
		deleteEndpoint.description("Unpublish the given node.");
		deleteEndpoint.path("/:uuid/published");
		deleteEndpoint.method(DELETE);
		deleteEndpoint.produces(APPLICATION_JSON);
		deleteEndpoint.exampleResponse(200, nodeExamples.getNodeResponse2());
		deleteEndpoint.addQueryParameters(PublishParameters.class);
		deleteEndpoint.handler(rc -> {
			crudHandler.handleTakeOffline(InternalActionContext.create(rc), rc.request().getParam("uuid"));
		});

		// Language specific

		Endpoint getLanguageRoute = createEndpoint();
		getLanguageRoute.description("Return the published status for the given language of the node.");
		getLanguageRoute.path("/:uuid/languages/:languageTag/published");
		getLanguageRoute.method(GET);
		getLanguageRoute.produces(APPLICATION_JSON);
		getLanguageRoute.exampleResponse(200, versioningExamples.getPublishStatusModel());
		getLanguageRoute.handler(rc -> {
			crudHandler.handleGetPublishStatus(InternalActionContext.create(rc), rc.request().getParam("uuid"), rc.request().getParam("languageTag"));
		});

		Endpoint putLanguageRoute = createEndpoint();
		putLanguageRoute.path("/:uuid/languages/:languageTag/published").method(PUT).produces(APPLICATION_JSON);
		putLanguageRoute.description("Publish the language of the node.");
		putLanguageRoute.exampleResponse(200, versioningExamples.getPublishStatusModel());
		putLanguageRoute.produces(APPLICATION_JSON);
		putLanguageRoute.handler(rc -> {
			crudHandler.handlePublish(InternalActionContext.create(rc), rc.request().getParam("uuid"), rc.request().getParam("languageTag"));
		});

		Endpoint deleteLanguageRoute = createEndpoint();
		deleteLanguageRoute.description("Take the language of the node offline.");
		deleteLanguageRoute.path("/:uuid/languages/:languageTag/published").method(DELETE).produces(APPLICATION_JSON);
		deleteLanguageRoute.exampleResponse(200, versioningExamples.getPublishStatusModel());
		deleteLanguageRoute.produces(APPLICATION_JSON);
		deleteLanguageRoute.handler(rc -> {
			crudHandler.handleTakeOffline(InternalActionContext.create(rc), rc.request().getParam("uuid"), rc.request().getParam("languageTag"));
		});

	}

	public NodeCrudHandler getCrudHandler() {
		return crudHandler;
	}
}
