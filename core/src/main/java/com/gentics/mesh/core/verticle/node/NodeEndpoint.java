package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.raml.model.Resource;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.impl.NavigationParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.MultiMap;

/**
 * The content verticle adds rest endpoints for manipulating nodes.
 */
public class NodeEndpoint extends AbstractProjectEndpoint {

	private NodeCrudHandler crudHandler;

	private Resource resource = new Resource();

	private BinaryFieldHandler binaryFieldHandler;

	public NodeEndpoint() {
		super("nodes", null);
	}

	@Inject
	public NodeEndpoint(BootstrapInitializer boot, NodeCrudHandler crudHandler, BinaryFieldHandler fieldAPIHandler) {
		super("nodes", boot);
		this.crudHandler = crudHandler;
		this.binaryFieldHandler = fieldAPIHandler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of nodes.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();
		if (getCrudHandler() != null) {
			route("/:nodeUuid").handler(getCrudHandler().getUuidHandler("node_not_found_for_uuid"));
		}

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();

		// sub handlers
		addChildrenHandler();
		addTagsHandler();
		addMoveHandler();
		addBinaryHandlers();
		addLanguageHandlers();
		addNavigationHandlers();
		addPublishHandlers();
	}

	public Resource getResource() {
		return resource;
	}

	private void addNavigationHandlers() {

		NavigationResponse responseExample = new NavigationResponse();
		responseExample.setUuid(UUIDUtil.randomUUID());

		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:nodeUuid/navigation");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Returns a navigation object for the provided node.");
		endpoint.displayName("Navigation");
		endpoint.exampleResponse(OK, responseExample, "Loaded navigation.");
		endpoint.addQueryParameters(NavigationParametersImpl.class);
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("nodeUuid");
			crudHandler.handleNavigation(ac, uuid);
		});
	}

	private void addLanguageHandlers() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:nodeUuid/languages/:language");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		endpoint.addUriParameter("language", "Language tag of the content which should be deleted.", "en");
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Delete the language specific content of the node.");
		endpoint.exampleResponse(NO_CONTENT, "Language variation of the node has been deleted.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("nodeUuid");
			String languageTag = ac.getParameter("language");
			crudHandler.handleDeleteLanguage(ac, uuid, languageTag);
		});
	}

	private void addBinaryHandlers() {
		InternalEndpointRoute fieldUpdate = createRoute();
		fieldUpdate.path("/:nodeUuid/binary/:fieldName");
		fieldUpdate.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		fieldUpdate.addUriParameter("fieldName", "Name of the field which should be created.", "stringField");
		fieldUpdate.method(POST);
		fieldUpdate.produces(APPLICATION_JSON);
		fieldUpdate.exampleRequest(nodeExamples.getExampleBinaryUploadFormParameters());
		fieldUpdate.exampleResponse(OK, nodeExamples.getNodeResponseWithAllFields(), "The response contains the updated node.");
		fieldUpdate.description("Update the binaryfield with the given name.");
		fieldUpdate.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			MultiMap attributes = rc.request().formAttributes();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			binaryFieldHandler.handleUpdateField(ac, uuid, fieldName, attributes);
		}, false);

		InternalEndpointRoute imageTransform = createRoute();
		imageTransform.path("/:nodeUuid/binaryTransform/:fieldName");
		imageTransform.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		imageTransform.addUriParameter("fieldName", "Name of the field", "image");
		imageTransform.method(POST);
		imageTransform.produces(APPLICATION_JSON);
		imageTransform.consumes(APPLICATION_JSON);
		imageTransform.description("Transform the image with the given field name and overwrite the stored image with the transformation result.");
		imageTransform.exampleRequest(nodeExamples.getBinaryFieldTransformRequest());
		imageTransform.exampleResponse(OK, nodeExamples.getNodeResponseWithAllFields(), "Transformation was executed and updated node was returned.");
		imageTransform.handler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			binaryFieldHandler.handleTransformImage(rc, uuid, fieldName);
		});

		InternalEndpointRoute fieldGet = createRoute();
		fieldGet.path("/:nodeUuid/binary/:fieldName");
		fieldGet.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		fieldGet.addUriParameter("fieldName", "Name of the binary field", "image");
		fieldGet.addQueryParameters(ImageManipulationParametersImpl.class);
		fieldGet.method(GET);
		fieldGet.description(
				"Download the binary field with the given name. You can use image query parameters for crop and resize if the binary data represents an image.");
		fieldGet.handler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			binaryFieldHandler.handleReadBinaryField(rc, uuid, fieldName);
		});

	}

	private void addMoveHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:nodeUuid/moveTo/:toUuid");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node which should be moved.", UUIDUtil.randomUUID());
		endpoint.addUriParameter("toUuid", "Uuid of target the node.", UUIDUtil.randomUUID());
		endpoint.method(POST);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Move the node into the target node.");
		endpoint.exampleResponse(NO_CONTENT, "Node was moved.");
		endpoint.addQueryParameters(VersioningParametersImpl.class);
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("nodeUuid");
			String toUuid = ac.getParameter("toUuid");
			crudHandler.handleMove(ac, uuid, toUuid);
		});
	}

	private void addChildrenHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:nodeUuid/children");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, nodeExamples.getNodeListResponse(), "List of loaded node children.");
		endpoint.description("Load all child nodes and return a paged list response.");
		endpoint.addQueryParameters(PagingParametersImpl.class);
		endpoint.addQueryParameters(NodeParametersImpl.class);
		endpoint.addQueryParameters(VersioningParametersImpl.class);
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("nodeUuid");
			crudHandler.handleReadChildren(ac, uuid);
		});
	}

	// TODO filtering, sorting
	private void addTagsHandler() {
		InternalEndpointRoute getTags = createRoute();
		getTags.path("/:nodeUuid/tags");
		getTags.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		getTags.method(GET);
		getTags.produces(APPLICATION_JSON);
		getTags.exampleResponse(OK, tagExamples.createTagListResponse(), "List of tags that were used to tag the node.");
		getTags.description("Return a list of all tags which tag the node.");
		getTags.addQueryParameters(VersioningParametersImpl.class);
		getTags.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("nodeUuid");
			crudHandler.readTags(ac, uuid);
		});

		InternalEndpointRoute bulkUpdate = createRoute();
		bulkUpdate.path("/:nodeUuid/tags");
		bulkUpdate.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		bulkUpdate.method(POST);
		bulkUpdate.produces(APPLICATION_JSON);
		bulkUpdate.description("Update the list of assigned tags");
		bulkUpdate.exampleRequest(tagExamples.getTagListUpdateRequest());
		bulkUpdate.exampleResponse(OK, tagExamples.createTagListResponse(), "Updated tag list.");
		bulkUpdate.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String nodeUuid = ac.getParameter("nodeUuid");
			crudHandler.handleBulkTagUpdate(ac, nodeUuid);
		});

		InternalEndpointRoute addTag = createRoute();
		addTag.path("/:nodeUuid/tags/:tagUuid");
		addTag.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		addTag.addUriParameter("tagUuid", "Uuid of the tag", UUIDUtil.randomUUID());
		addTag.method(POST);
		addTag.produces(APPLICATION_JSON);
		addTag.exampleResponse(OK, nodeExamples.getNodeResponse2(), "Updated node.");
		addTag.description("Assign the given tag to the node.");
		addTag.addQueryParameters(VersioningParametersImpl.class);
		addTag.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("nodeUuid");
			String tagUuid = ac.getParameter("tagUuid");
			crudHandler.handleAddTag(ac, uuid, tagUuid);
		});

		// TODO fix error handling. This does not fail when tagUuid could not be found
		InternalEndpointRoute removeTag = createRoute();
		removeTag.path("/:nodeUuid/tags/:tagUuid");
		removeTag.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		removeTag.addUriParameter("tagUuid", "Uuid of the tag", UUIDUtil.randomUUID());
		removeTag.method(DELETE);
		removeTag.produces(APPLICATION_JSON);
		removeTag.description("Remove the given tag from the node.");
		removeTag.exampleResponse(NO_CONTENT, "Removal was successful.");
		removeTag.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("nodeUuid");
			String tagUuid = ac.getParameter("tagUuid");
			crudHandler.handleRemoveTag(ac, uuid, tagUuid);
		});

	}

	// TODO handle schema by name / by uuid - move that code in a separate
	// handler
	private void addCreateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new node.");
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(nodeExamples.getNodeCreateRequest());
		endpoint.exampleResponse(CREATED, nodeExamples.getNodeResponseWithAllFields(), "Created node.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			ac.getVersioningParameters().setVersion("draft");
			crudHandler.handleCreate(ac);
		});
	}

	private void addReadHandler() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:nodeUuid");
		readOne.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		readOne.method(GET);
		readOne.description("Load the node with the given uuid.");
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, nodeExamples.getNodeResponseWithAllFields(), "Loaded node.");
		readOne.addQueryParameters(VersioningParametersImpl.class);
		readOne.addQueryParameters(RolePermissionParametersImpl.class);
		readOne.addQueryParameters(NodeParametersImpl.class);
		readOne.handler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
				crudHandler.handleRead(ac, uuid);
			}
		});

		InternalEndpointRoute readAll = createRoute();
		readAll.path("/");
		readAll.description("Read all nodes and return a paged list response.");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
		readAll.exampleResponse(OK, nodeExamples.getNodeListResponse(), "Loaded list of nodes.");
		readAll.addQueryParameters(VersioningParametersImpl.class);
		readAll.addQueryParameters(RolePermissionParametersImpl.class);
		readAll.addQueryParameters(NodeParametersImpl.class);
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			crudHandler.handleReadList(ac);
		});

	}

	private void addDeleteHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:nodeUuid");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", UUIDUtil.randomUUID());
		endpoint.description("Delete the node with the given uuid.");
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.addQueryParameters(DeleteParametersImpl.class);
		endpoint.exampleResponse(NO_CONTENT, "Deletion was successful.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
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
		InternalEndpointRoute endpoint = createRoute();
		endpoint.description("Update the node with the given uuid. It is mandatory to specify the version within the update request. "
				+ "Mesh will automatically check for version conflicts and return a 409 error if a conflict has been detected. "
				+ "Additional conflict checks for WebRoot path conflicts will also be performed. The node is created if no node with the specified uuid could be found.");
		endpoint.path("/:nodeUuid");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(nodeExamples.getNodeUpdateRequest());
		endpoint.exampleResponse(OK, nodeExamples.getNodeResponse2(), "Updated node.");
		endpoint.exampleResponse(CONFLICT, miscExamples.createMessageResponse(), "A conflict has been detected.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("nodeUuid");
			ac.getVersioningParameters().setVersion("draft");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addPublishHandlers() {

		InternalEndpointRoute getEndpoint = createRoute();
		getEndpoint.description("Return the published status of the node.");
		getEndpoint.path("/:nodeUuid/published");
		getEndpoint.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		getEndpoint.method(GET);
		getEndpoint.produces(APPLICATION_JSON);
		getEndpoint.exampleResponse(OK, versioningExamples.createPublishStatusResponse(), "Publish status of the node.");
		getEndpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = rc.request().getParam("nodeUuid");
			crudHandler.handleGetPublishStatus(ac, uuid);
		});

		InternalEndpointRoute putEndpoint = createRoute();
		putEndpoint.description("Publish all language specific contents of the node with the given uuid.");
		putEndpoint.path("/:nodeUuid/published");
		putEndpoint.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		putEndpoint.method(POST);
		putEndpoint.produces(APPLICATION_JSON);
		putEndpoint.exampleResponse(OK, versioningExamples.createPublishStatusResponse(), "Publish status of the node.");
		putEndpoint.addQueryParameters(PublishParametersImpl.class);
		putEndpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = rc.request().getParam("nodeUuid");
			crudHandler.handlePublish(ac, uuid);
		});

		InternalEndpointRoute deleteEndpoint = createRoute();
		deleteEndpoint.description("Unpublish the given node.");
		deleteEndpoint.path("/:nodeUuid/published");
		deleteEndpoint.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		deleteEndpoint.method(DELETE);
		deleteEndpoint.produces(APPLICATION_JSON);
		deleteEndpoint.exampleResponse(NO_CONTENT, "Node was unpublished.");
		deleteEndpoint.addQueryParameters(PublishParametersImpl.class);
		deleteEndpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = rc.request().getParam("nodeUuid");
			crudHandler.handleTakeOffline(ac, uuid);
		});

		// Language specific

		InternalEndpointRoute getLanguageRoute = createRoute();
		getLanguageRoute.description("Return the publish status for the given language of the node.");
		getLanguageRoute.path("/:nodeUuid/languages/:language/published");
		getLanguageRoute.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		getLanguageRoute.addUriParameter("language", "Name of the language tag", "en");
		getLanguageRoute.method(GET);
		getLanguageRoute.produces(APPLICATION_JSON);
		getLanguageRoute.exampleResponse(OK, versioningExamples.createPublishStatusModel(), "Publish status of the specific language.");
		getLanguageRoute.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = rc.request().getParam("nodeUuid");
			String lang = rc.request().getParam("language");
			crudHandler.handleGetPublishStatus(ac, uuid, lang);
		});

		InternalEndpointRoute putLanguageRoute = createRoute();
		putLanguageRoute.path("/:nodeUuid/languages/:language/published").method(POST).produces(APPLICATION_JSON);
		putLanguageRoute.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		putLanguageRoute.addUriParameter("language", "Name of the language tag", "en");
		putLanguageRoute.description(
				"Publish the language of the node. This will automatically assign a new major version to the node and update the draft version to the published version.");
		putLanguageRoute.exampleResponse(OK, versioningExamples.createPublishStatusModel(), "Updated publish status.");
		putLanguageRoute.produces(APPLICATION_JSON);
		putLanguageRoute.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = rc.request().getParam("nodeUuid");
			String lang = rc.request().getParam("language");
			crudHandler.handlePublish(ac, uuid, lang);
		});

		InternalEndpointRoute deleteLanguageRoute = createRoute();
		deleteLanguageRoute.description("Take the language of the node offline.");
		deleteLanguageRoute.path("/:nodeUuid/languages/:language/published").method(DELETE).produces(APPLICATION_JSON);
		deleteLanguageRoute.addUriParameter("nodeUuid", "Uuid of the node", UUIDUtil.randomUUID());
		deleteLanguageRoute.addUriParameter("language", "Name of the language tag", "en");
		deleteLanguageRoute.exampleResponse(NO_CONTENT, versioningExamples.createPublishStatusModel(), "Node language was taken offline.");
		deleteLanguageRoute.produces(APPLICATION_JSON);
		deleteLanguageRoute.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = rc.request().getParam("nodeUuid");
			String lang = rc.request().getParam("language");
			crudHandler.handleTakeOffline(ac, uuid, lang);
		});

	}

	public NodeCrudHandler getCrudHandler() {
		return crudHandler;
	}
}
