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
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
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

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of nodes.";
	}

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
		endpoint.path("/:nodeUuid/navigation");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Returns a navigation object for the provided node.");
		endpoint.displayName("Navigation");
		endpoint.exampleRequest(new GenericMessageResponse("test"));
		endpoint.exampleResponse(200, responseExample);
		endpoint.addQueryParameters(NavigationParameters.class);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("nodeUuid");
			crudHandler.handleNavigation(ac, uuid);
		});
	}

	private void addLanguageHandlers() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:nodeUuid/languages/:language");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		endpoint.addUriParameter("language", "Language tag of the content which should be deleted.", "en");
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Delete the language specific content of the node.");
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("nodeUuid");
			String languageTag = ac.getParameter("language");
			crudHandler.handleDeleteLanguage(ac, uuid, languageTag);
		});
	}

	private void addFieldHandlers() {
		Endpoint fieldUpdate = createEndpoint();
		fieldUpdate.path("/:nodeUuid/languages/:language/fields/:field");
		fieldUpdate.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		fieldUpdate.addUriParameter("language", "Language tag of the content which contains the field.", "en");
		fieldUpdate.addUriParameter("field", "Name of the field which should be created.", "stringField");
		fieldUpdate.method(POST);
		fieldUpdate.produces(APPLICATION_JSON);
		// TODO consumes json and upload
		fieldUpdate.exampleResponse(200, miscExamples.getMessageResponse());
		fieldUpdate.description("Update the field with the given name.");
		fieldUpdate.handler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String languageTag = rc.request().getParam("language");
			String fieldName = rc.request().getParam("field");
			InternalActionContext ac = InternalActionContext.create(rc);
			fieldAPIHandler.handleCreateField(ac, uuid, languageTag, fieldName);
		});

		Endpoint fieldGet = createEndpoint();
		fieldGet.path("/:nodeUuid/languages/:language/fields/:field");
		fieldGet.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		fieldGet.addUriParameter("language", "Language tag.", "en");
		fieldGet.addUriParameter("field", "Name of the field", "content");
		fieldGet.method(GET);
		fieldGet.description("Load the field with the given name.");
		fieldGet.handler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String languageTag = rc.request().getParam("language");
			String fieldName = rc.request().getParam("field");
			fieldAPIHandler.handleReadField(rc, uuid, languageTag, fieldName);
		});

		Endpoint fieldCreate = createEndpoint();
		fieldCreate.path("/:nodeUuid/languages/:language/fields/:field");
		fieldCreate.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		fieldCreate.addUriParameter("language", "Language tag.", "en");
		fieldCreate.addUriParameter("field", "Name of the field", "binary");
		fieldCreate.description("Create a new field with the given name.");
		fieldCreate.method(POST).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String languageTag = rc.request().getParam("language");
			String fieldName = rc.request().getParam("field");
			InternalActionContext ac = InternalActionContext.create(rc);
			fieldAPIHandler.handleUpdateField(ac, uuid, languageTag, fieldName);
		});

		Endpoint fieldDelete = createEndpoint();
		fieldDelete.path("/:nodeUuid/languages/:language/fields/:field");
		fieldDelete.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		fieldDelete.addUriParameter("language", "Language tag.", "en");
		fieldDelete.addUriParameter("field", "Name of the field", "content");
		fieldDelete.method(DELETE);
		fieldDelete.produces(APPLICATION_JSON);
		fieldDelete.description("Delete the field with the given name");
		fieldDelete.exampleResponse(200, miscExamples.getMessageResponse());
		fieldDelete.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("nodeUuid");
			String languageTag = ac.getParameter("language");
			String fieldName = ac.getParameter("field");
			fieldAPIHandler.handleRemoveField(ac, uuid, languageTag, fieldName);
		});

		// Image Transformation
		Endpoint imageTransform = createEndpoint();
		imageTransform.path("/:nodeUuid/languages/:language/fields/:field/transform");
		imageTransform.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		imageTransform.addUriParameter("language", "Language tag.", "en");
		imageTransform.addUriParameter("field", "Name of the field", "image");
		imageTransform.method(POST);
		imageTransform.produces(APPLICATION_JSON);
		imageTransform.consumes(APPLICATION_JSON);
		imageTransform.description("Transform the image with the given field name and overwrite the stored image with the transformation result.");
		imageTransform.exampleRequest(nodeExamples.getBinaryFieldTransformRequest());
		imageTransform.exampleResponse(200, miscExamples.getMessageResponse());
		imageTransform.handler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String languageTag = rc.request().getParam("language");
			String fieldName = rc.request().getParam("field");
			fieldAPIHandler.handleTransformImage(rc, uuid, languageTag, fieldName);
		});

		// List methods
		Endpoint listItemDelete = createEndpoint();
		listItemDelete.path("/:nodeUuid/languages/:language/fields/:field/:itemIndex");
		listItemDelete.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		listItemDelete.addUriParameter("language", "Language tag.", "en");
		listItemDelete.addUriParameter("field", "Name of the field", "stringList");
		listItemDelete.addUriParameter("itemIndex", "Index which identifies the item which should be deleted", "5");
		listItemDelete.method(DELETE);
		listItemDelete.produces(APPLICATION_JSON);
		listItemDelete.exampleResponse(200, miscExamples.getMessageResponse());
		listItemDelete.description("Delete the field list item at the given index position (Not yet implemented)");
		listItemDelete.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = rc.request().getParam("nodeUuid");
			String languageTag = rc.request().getParam("language");
			String fieldName = rc.request().getParam("field");
			fieldAPIHandler.handleRemoveFieldItem(ac, uuid);
		});

		Endpoint listItemGet = createEndpoint();
		listItemGet.path("/:nodeUuid/languages/:language/fields/:field/:itemIndex");
		listItemGet.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		listItemGet.addUriParameter("language", "Language tag.", "en");
		listItemGet.addUriParameter("field", "Name of the field", "stringList");
		listItemGet.addUriParameter("itemIndex", "Index which identifies the item which should be loaded.", "5");
		listItemGet.method(GET);
		listItemGet.exampleResponse(200, miscExamples.getMessageResponse());
		listItemGet.description("Load the field list item at the given index position. (Not yet implemented)");
		listItemGet.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = rc.request().getParam("nodeUuid");
			String languageTag = rc.request().getParam("language");
			String fieldName = rc.request().getParam("field");
			fieldAPIHandler.handleReadFieldItem(ac, uuid);
		});

		Endpoint updateFieldItem = createEndpoint();
		updateFieldItem.path("/:nodeUuid/languages/:language/fields/:field/:itemIndex");
		updateFieldItem.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		updateFieldItem.addUriParameter("language", "Language tag.", "en");
		updateFieldItem.addUriParameter("field", "Name of the field", "stringList");
		updateFieldItem.addUriParameter("itemIndex", "Index which identifies the item which should be created.", "5");
		updateFieldItem.method(POST);
		updateFieldItem.description("Update the field list item at the given index position. (Not yet implemented)");
		updateFieldItem.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = rc.request().getParam("nodeUuid");
			String languageTag = rc.request().getParam("language");
			String fieldName = rc.request().getParam("field");
			fieldAPIHandler.handleUpdateFieldItem(ac, uuid);
		});

		Endpoint moveFieldItem = createEndpoint();
		moveFieldItem.path("/:nodeUuid/languages/:language/fields/:field/:itemIndex/move/:newItemIndex");
		moveFieldItem.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		moveFieldItem.addUriParameter("language", "Language tag.", "en");
		moveFieldItem.addUriParameter("field", "Name of the field", "stringList");
		moveFieldItem.addUriParameter("itemIndex", "Index which identifies the item which should be moved.", "5");
		moveFieldItem.addUriParameter("newItemIndex", "Index which identifies the item which should be moved to.", "6");
		moveFieldItem.method(POST);
		moveFieldItem.description("Move the field list item on a new index position. (Not yet implemented)");
		moveFieldItem.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = rc.request().getParam("nodeUuid");
			String languageTag = rc.request().getParam("language");
			String fieldName = rc.request().getParam("field");
			fieldAPIHandler.handleMoveFieldItem(ac, uuid);
		});

		// TODO copy?
		// route("/:uuid/fields/:field/:itemIndex/copy/:newItemIndex").method(POST).handler(rc -> {
		// crudHandler.handleMoveFieldItem(ActionContext.create(rc));
		// });

	}

	private void addMoveHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:nodeUuid/moveTo/:toUuid");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node which should be moved.", UUIDUtil.randomUUID());
		endpoint.addUriParameter("toUuid", "Uuid of target the node.", UUIDUtil.randomUUID());
		endpoint.method(POST);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Move the node into the target node.");
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
		endpoint.addQueryParameters(VersioningParameters.class);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("nodeUuid");
			String toUuid = ac.getParameter("toUuid");
			crudHandler.handleMove(ac, uuid, toUuid);
		});
	}

	private void addChildrenHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:nodeUuid/children");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(200, nodeExamples.getNodeListResponse());
		endpoint.description("Load all child nodes and return a paged list response.");
		endpoint.addQueryParameters(PagingParameters.class);
		endpoint.addQueryParameters(NodeParameters.class);
		endpoint.addQueryParameters(VersioningParameters.class);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("nodeUuid");
			crudHandler.handleReadChildren(ac, uuid);
		});
	}

	// TODO filtering, sorting
	private void addTagsHandler() {
		Endpoint getTags = createEndpoint();
		getTags.path("/:nodeUuid/tags");
		getTags.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		getTags.method(GET);
		getTags.produces(APPLICATION_JSON);
		getTags.exampleResponse(200, tagExamples.getTagListResponse());
		getTags.description("Return a list of all tags which tag the node.");
		getTags.addQueryParameters(VersioningParameters.class);
		getTags.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("nodeUuid");
			crudHandler.readTags(ac, uuid);
		});

		Endpoint addTag = createEndpoint();
		addTag.path("/:nodeUuid/tags/:tagUuid");
		addTag.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		addTag.addUriParameter("tagUuid", "Uuid of the tag", UUIDUtil.randomUUID());
		addTag.method(POST);
		addTag.produces(APPLICATION_JSON);
		addTag.exampleResponse(200, nodeExamples.getNodeResponse2());
		addTag.description("Assign the given tag to the node.");
		addTag.addQueryParameters(VersioningParameters.class);
		addTag.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("nodeUuid");
			String tagUuid = ac.getParameter("tagUuid");
			crudHandler.handleAddTag(ac, uuid, tagUuid);
		});

		// TODO fix error handling. This does not fail when tagUuid could not be found
		Endpoint removeTag = createEndpoint();
		removeTag.path("/:nodeUuid/tags/:tagUuid");
		removeTag.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		removeTag.addUriParameter("tagUuid", "Uuid of the tag", UUIDUtil.randomUUID());
		removeTag.method(DELETE);
		removeTag.produces(APPLICATION_JSON);
		removeTag.description("Remove the given tag from the node.");
		removeTag.exampleResponse(200, nodeExamples.getNodeResponse2());
		removeTag.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("nodeUuid");
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

	private void addReadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:nodeUuid");
		readOne.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		readOne.method(GET);
		readOne.description("Load the node with the given uuid.");
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(200, nodeExamples.getNodeResponseWithAllFields());
		readOne.addQueryParameters(VersioningParameters.class);
		readOne.addQueryParameters(RolePermissionParameters.class);
		readOne.addQueryParameters(NodeParameters.class);
		readOne.handler(rc -> {
			String uuid = rc.request().params().get("nodeUuid");
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
		readAll.addQueryParameters(RolePermissionParameters.class);
		readAll.addQueryParameters(NodeParameters.class);
		readAll.addQueryParameters(PagingParameters.class);
		readAll.handler(rc -> crudHandler.handleReadList(InternalActionContext.create(rc)));

	}

	private void addDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:nodeUuid");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		endpoint.description("Delete the node with the given uuid.");
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(204);
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("nodeUuid");
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
		endpoint.description("Update the node with the given uuid. It is mandatory to specify the version within the update request. "
				+ "Mesh will automatically check for version conflicts and return a 409 error if a conflict has been detected. "
				+ "Additional conflict checks for webrootpath conflicts will also be performed.");
		endpoint.path("/:nodeUuid");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(nodeExamples.getNodeUpdateRequest());
		endpoint.exampleResponse(200, nodeExamples.getNodeResponse2());
		endpoint.exampleResponse(409, miscExamples.getMessageResponse());
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("nodeUuid");
			ac.getVersioningParameters().setVersion("draft");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addPublishHandlers() {

		Endpoint getEndpoint = createEndpoint();
		getEndpoint.description("Return the published status of the node.");
		getEndpoint.path("/:nodeUuid/published");
		getEndpoint.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		getEndpoint.method(GET);
		getEndpoint.produces(APPLICATION_JSON);
		getEndpoint.exampleResponse(200, versioningExamples.createPublishStatusResponse());
		getEndpoint.handler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			crudHandler.handleGetPublishStatus(InternalActionContext.create(rc), uuid);
		});

		Endpoint putEndpoint = createEndpoint();
		putEndpoint.description("Publish the node with the given uuid.");
		putEndpoint.path("/:nodeUuid/published");
		putEndpoint.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		putEndpoint.method(POST);
		putEndpoint.produces(APPLICATION_JSON);
		putEndpoint.exampleResponse(200, versioningExamples.createPublishStatusResponse());
		putEndpoint.addQueryParameters(PublishParameters.class);
		putEndpoint.handler(rc -> {
			crudHandler.handlePublish(InternalActionContext.create(rc), rc.request().getParam("nodeUuid"));
		});

		Endpoint deleteEndpoint = createEndpoint();
		deleteEndpoint.description("Unpublish the given node.");
		deleteEndpoint.path("/:nodeUuid/published");
		deleteEndpoint.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		deleteEndpoint.method(DELETE);
		deleteEndpoint.produces(APPLICATION_JSON);
		deleteEndpoint.exampleResponse(200, nodeExamples.getNodeResponse2());
		deleteEndpoint.addQueryParameters(PublishParameters.class);
		deleteEndpoint.handler(rc -> {
			crudHandler.handleTakeOffline(InternalActionContext.create(rc), rc.request().getParam("nodeUuid"));
		});

		// Language specific

		Endpoint getLanguageRoute = createEndpoint();
		getLanguageRoute.description("Return the publish status for the given language of the node.");
		getLanguageRoute.path("/:nodeUuid/languages/:language/published");
		getLanguageRoute.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		getLanguageRoute.addUriParameter("language", "Name of the language tag", "en");
		getLanguageRoute.method(GET);
		getLanguageRoute.produces(APPLICATION_JSON);
		getLanguageRoute.exampleResponse(200, versioningExamples.createPublishStatusModel());
		getLanguageRoute.handler(rc -> {
			crudHandler.handleGetPublishStatus(InternalActionContext.create(rc), rc.request().getParam("nodeUuid"),
					rc.request().getParam("language"));
		});

		Endpoint putLanguageRoute = createEndpoint();
		putLanguageRoute.path("/:nodeUuid/languages/:language/published").method(POST).produces(APPLICATION_JSON);
		putLanguageRoute.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		putLanguageRoute.addUriParameter("language", "Name of the language tag", "en");
		putLanguageRoute.description(
				"Publish the language of the node. This will automatically assign a new major version to the node and update the draft version to the published version.");
		putLanguageRoute.exampleResponse(200, versioningExamples.createPublishStatusModel());
		putLanguageRoute.produces(APPLICATION_JSON);
		putLanguageRoute.handler(rc -> {
			crudHandler.handlePublish(InternalActionContext.create(rc), rc.request().getParam("nodeUuid"), rc.request().getParam("language"));
		});

		Endpoint deleteLanguageRoute = createEndpoint();
		deleteLanguageRoute.description("Take the language of the node offline.");
		deleteLanguageRoute.path("/:nodeUuid/languages/:language/published").method(DELETE).produces(APPLICATION_JSON);
		deleteLanguageRoute.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		deleteLanguageRoute.addUriParameter("language", "Name of the language tag", "en");
		deleteLanguageRoute.exampleResponse(200, versioningExamples.createPublishStatusModel());
		deleteLanguageRoute.produces(APPLICATION_JSON);
		deleteLanguageRoute.handler(rc -> {
			crudHandler.handleTakeOffline(InternalActionContext.create(rc), rc.request().getParam("nodeUuid"), rc.request().getParam("language"));
		});

	}

	public NodeCrudHandler getCrudHandler() {
		return crudHandler;
	}
}
