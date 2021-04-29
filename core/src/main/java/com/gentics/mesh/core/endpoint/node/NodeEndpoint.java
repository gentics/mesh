package com.gentics.mesh.core.endpoint.node;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.impl.NavigationParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;
import io.vertx.core.MultiMap;
import org.apache.commons.lang3.StringUtils;
import org.raml.model.Resource;

import javax.inject.Inject;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_MOVED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_TAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNPUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNTAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.example.ExampleUuids.NODE_DELOREAN_UUID;
import static com.gentics.mesh.example.ExampleUuids.TAG_RED_UUID;
import static com.gentics.mesh.example.ExampleUuids.UUID_1;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

/**
 * The content verticle adds rest endpoints for manipulating nodes.
 */
public class NodeEndpoint extends AbstractProjectEndpoint {

	private Resource resource = new Resource();

	private NodeCrudHandler crudHandler;

	private BinaryUploadHandlerImpl binaryUploadHandler;

	private BinaryTransformHandler binaryTransformHandler;

	private BinaryDownloadHandler binaryDownloadHandler;

	private S3BinaryUploadHandlerImpl s3binaryUploadHandler;

	public NodeEndpoint() {
		super("nodes", null, null);
	}

	@Inject
	public NodeEndpoint(MeshAuthChainImpl chain, BootstrapInitializer boot, NodeCrudHandler crudHandler, BinaryUploadHandlerImpl binaryUploadHandler,
		BinaryTransformHandler binaryTransformHandler, BinaryDownloadHandler binaryDownloadHandler, S3BinaryUploadHandlerImpl s3binaryUploadHandler) {
		super("nodes", chain, boot);
		this.crudHandler = crudHandler;
		this.binaryUploadHandler = binaryUploadHandler;
		this.binaryTransformHandler = binaryTransformHandler;
		this.binaryDownloadHandler = binaryDownloadHandler;
		this.s3binaryUploadHandler = s3binaryUploadHandler;
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
		addS3BinaryHandlers();
		addLanguageHandlers();
		addNavigationHandlers();
		addPublishHandlers();
		addVersioningHandlers();
	}

	public Resource getResource() {
		return resource;
	}

	private void addNavigationHandlers() {

		NavigationResponse responseExample = new NavigationResponse();
		responseExample.setUuid(UUID_1);

		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:nodeUuid/navigation");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Returns a navigation object for the provided node.");
		endpoint.displayName("Navigation");
		endpoint.exampleResponse(OK, responseExample, "Loaded navigation.");
		endpoint.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		endpoint.addQueryParameters(NavigationParametersImpl.class);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("nodeUuid");
			crudHandler.handleNavigation(ac, uuid);
		});
	}

	private void addVersioningHandlers() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:nodeUuid/versions");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Returns a list of versions.");
		endpoint.displayName("Versions");
		endpoint.exampleResponse(OK, nodeExamples.createVersionsList(), "Loaded version list.");
		endpoint.addQueryParameters(NodeParametersImpl.class);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("nodeUuid");
			crudHandler.handleListVersions(ac, uuid);
		});
	}

	private void addLanguageHandlers() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:nodeUuid/languages/:language");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		endpoint.addUriParameter("language", "Language tag of the content which should be deleted.", "en");
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Delete the language specific content of the node.");
		endpoint.exampleResponse(NO_CONTENT, "Language variation of the node has been deleted.");
		endpoint.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		endpoint.events(NODE_CONTENT_DELETED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("nodeUuid");
			String languageTag = ac.getParameter("language");
			crudHandler.handleDeleteLanguage(ac, uuid, languageTag);
		});
	}

	private void addBinaryHandlers() {
		InternalEndpointRoute fieldUpdate = createRoute();
		fieldUpdate.path("/:nodeUuid/binary/:fieldName");
		fieldUpdate.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		fieldUpdate.addUriParameter("fieldName", "Name of the field which should be created.", "stringField");
		fieldUpdate.method(POST);
		fieldUpdate.produces(APPLICATION_JSON);
		fieldUpdate.exampleRequest(nodeExamples.getExampleBinaryUploadFormParameters());
		fieldUpdate.exampleResponse(OK, nodeExamples.getNodeResponseWithAllFields(), "The response contains the updated node.");
		fieldUpdate.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node or the field could not be found.");
		fieldUpdate.description("Update the binaryfield with the given name.");
		fieldUpdate.events(NODE_UPDATED);
		fieldUpdate.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			MultiMap attributes = rc.request().formAttributes();
			InternalActionContext ac = wrap(rc);
			binaryUploadHandler.handleUpdateField(ac, uuid, fieldName, attributes);
		});

		InternalEndpointRoute imageTransform = createRoute();
		imageTransform.path("/:nodeUuid/binaryTransform/:fieldName");
		imageTransform.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		imageTransform.addUriParameter("fieldName", "Name of the field", "image");
		imageTransform.method(POST);
		imageTransform.produces(APPLICATION_JSON);
		imageTransform.consumes(APPLICATION_JSON);
		imageTransform.description("Transform the image with the given field name and overwrite the stored image with the transformation result.");
		imageTransform.exampleRequest(nodeExamples.getBinaryFieldTransformRequest());
		imageTransform.exampleResponse(OK, nodeExamples.getNodeResponseWithAllFields(), "Transformation was executed and updated node was returned.");
		imageTransform.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node or the field could not be found.");
		imageTransform.events(NODE_UPDATED);
		imageTransform.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			binaryTransformHandler.handle(rc, uuid, fieldName);
		});

		InternalEndpointRoute fieldGet = createRoute();
		fieldGet.path("/:nodeUuid/binary/:fieldName");
		fieldGet.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		fieldGet.addUriParameter("fieldName", "Name of the binary field", "image");
		fieldGet.addQueryParameters(ImageManipulationParametersImpl.class);
		fieldGet.addQueryParameters(VersioningParametersImpl.class);
		fieldGet.method(GET);
		fieldGet.description(
			"Download the binary field with the given name. You can use image query parameters for crop and resize if the binary data represents an image.");
		fieldGet.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			binaryDownloadHandler.handleReadBinaryField(rc, uuid, fieldName);
		});

	}

	private void addS3BinaryHandlers() {
		InternalEndpointRoute fieldUpdate = createRoute();
		fieldUpdate.path("/:nodeUuid/s3binary/:fieldName");
		fieldUpdate.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		fieldUpdate.addUriParameter("fieldName", "Name of the field which should be created.", "stringField");
		fieldUpdate.method(POST);
		fieldUpdate.produces(APPLICATION_JSON);
		fieldUpdate.exampleRequest(nodeExamples.getExampleBinaryUploadFormParameters());
		fieldUpdate.exampleResponse(OK, nodeExamples.getNodeResponseWithAllFields(), "The response contains the updated node.");
		fieldUpdate.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node or the field could not be found.");
		fieldUpdate.description("Update the binaryfield with the given name.");
		fieldUpdate.events(NODE_UPDATED);
		fieldUpdate.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			InternalActionContext ac = wrap(rc);
			s3binaryUploadHandler.handleUpdateField(ac, uuid, fieldName);
		});
	}

	private void addMoveHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:nodeUuid/moveTo/:toUuid");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node which should be moved.", NODE_DELOREAN_UUID);
		endpoint.addUriParameter("toUuid", "Uuid of target the node.", TAG_RED_UUID);
		endpoint.method(POST);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Move the node into the target node.");
		endpoint.exampleResponse(NO_CONTENT, "Node was moved.");
		endpoint.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The source or target node could not be found.");
		endpoint.addQueryParameters(VersioningParametersImpl.class);
		endpoint.events(NODE_MOVED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("nodeUuid");
			String toUuid = ac.getParameter("toUuid");
			crudHandler.handleMove(ac, uuid, toUuid);
		});
	}

	private void addChildrenHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:nodeUuid/children");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleResponse(OK, nodeExamples.getNodeListResponse(), "List of loaded node children.");
		endpoint.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		endpoint.description("Load all child nodes and return a paged list response.");
		endpoint.addQueryParameters(PagingParametersImpl.class);
		endpoint.addQueryParameters(NodeParametersImpl.class);
		endpoint.addQueryParameters(VersioningParametersImpl.class);
		endpoint.addQueryParameters(GenericParametersImpl.class);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("nodeUuid");
			crudHandler.handleReadChildren(ac, uuid);
		});
	}

	// TODO filtering, sorting
	private void addTagsHandler() {
		InternalEndpointRoute getTags = createRoute();
		getTags.path("/:nodeUuid/tags");
		getTags.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		getTags.method(GET);
		getTags.produces(APPLICATION_JSON);
		getTags.exampleResponse(OK, tagExamples.createTagListResponse(), "List of tags that were used to tag the node.");
		getTags.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		getTags.description("Return a list of all tags which tag the node.");
		getTags.addQueryParameters(VersioningParametersImpl.class);
		getTags.addQueryParameters(GenericParametersImpl.class);
		getTags.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("nodeUuid");
			crudHandler.readTags(ac, uuid);
		});

		InternalEndpointRoute bulkUpdate = createRoute();
		bulkUpdate.path("/:nodeUuid/tags");
		bulkUpdate.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		bulkUpdate.method(POST);
		bulkUpdate.produces(APPLICATION_JSON);
		bulkUpdate.description("Update the list of assigned tags");
		bulkUpdate.exampleRequest(tagExamples.getTagListUpdateRequest());
		bulkUpdate.exampleResponse(OK, tagExamples.createTagListResponse(), "Updated tag list.");
		bulkUpdate.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		bulkUpdate.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String nodeUuid = ac.getParameter("nodeUuid");
			crudHandler.handleBulkTagUpdate(ac, nodeUuid);
		});

		InternalEndpointRoute addTag = createRoute();
		addTag.path("/:nodeUuid/tags/:tagUuid");
		addTag.addUriParameter("nodeUuid", "Uuid of the node", NODE_DELOREAN_UUID);
		addTag.addUriParameter("tagUuid", "Uuid of the tag", TAG_RED_UUID);
		addTag.method(POST);
		addTag.produces(APPLICATION_JSON);
		addTag.exampleResponse(OK, nodeExamples.getNodeResponse2(), "Updated node.");
		addTag.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node or tag could not be found.");
		addTag.description("Assign the given tag to the node.");
		addTag.addQueryParameters(VersioningParametersImpl.class);
		addTag.events(NODE_TAGGED);
		addTag.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("nodeUuid");
			String tagUuid = ac.getParameter("tagUuid");
			crudHandler.handleAddTag(ac, uuid, tagUuid);
		});

		// TODO fix error handling. This does not fail when tagUuid could not be found
		InternalEndpointRoute removeTag = createRoute();
		removeTag.path("/:nodeUuid/tags/:tagUuid");
		removeTag.addUriParameter("nodeUuid", "Uuid of the node", NODE_DELOREAN_UUID);
		removeTag.addUriParameter("tagUuid", "Uuid of the tag", TAG_RED_UUID);
		removeTag.method(DELETE);
		removeTag.produces(APPLICATION_JSON);
		removeTag.description("Remove the given tag from the node.");
		removeTag.exampleResponse(NO_CONTENT, "Removal was successful.");
		removeTag.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node or tag could not be found.");
		removeTag.events(NODE_UNTAGGED);
		removeTag.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
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
		endpoint.events(NODE_CREATED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			ac.getVersioningParameters().setVersion("draft");
			crudHandler.handleCreate(ac);
		});
	}

	private void addReadHandler() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:nodeUuid");
		readOne.addUriParameter("nodeUuid", "Uuid of the node", NODE_DELOREAN_UUID);
		readOne.method(GET);
		readOne.description("Load the node with the given uuid.");
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, nodeExamples.getNodeResponseWithAllFields(), "Loaded node.");
		readOne.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		readOne.addQueryParameters(VersioningParametersImpl.class);
		readOne.addQueryParameters(RolePermissionParametersImpl.class);
		readOne.addQueryParameters(NodeParametersImpl.class);
		readOne.addQueryParameters(GenericParametersImpl.class);
		readOne.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				InternalActionContext ac = wrap(rc);
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
		readAll.addQueryParameters(GenericParametersImpl.class);
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			crudHandler.handleReadList(ac);
		});

	}

	private void addDeleteHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:nodeUuid");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		endpoint.description("Delete the node with the given uuid.");
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.addQueryParameters(DeleteParametersImpl.class);
		endpoint.exampleResponse(NO_CONTENT, "Deletion was successful.");
		endpoint.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		endpoint.events(NODE_DELETED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("nodeUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	// TODO filter by project name
	// TODO use schema and only handle those i18n properties that were specified
	// within the schema.
	private void addUpdateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.description("Update or create the node with the given uuid. "
			+ "Mesh will automatically check for version conflicts if a version was specified in the request and return a 409 error if a conflict has been detected. "
			+ "Additional conflict checks for WebRoot path conflicts will also be performed. The node is created if no node with the specified uuid could be found.");
		endpoint.path("/:nodeUuid");
		endpoint.addUriParameter("nodeUuid", "Uuid of the node", NODE_DELOREAN_UUID);
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(nodeExamples.getNodeUpdateRequest());
		endpoint.exampleResponse(OK, nodeExamples.getNodeResponse2(), "Updated node.");
		endpoint.exampleResponse(CONFLICT, miscExamples.createMessageResponse(), "A conflict has been detected.");
		endpoint.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		endpoint.events(NODE_UPDATED, NODE_CREATED, NODE_CONTENT_CREATED, NODE_UPDATED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("nodeUuid");
			ac.getVersioningParameters().setVersion("draft");
			crudHandler.handleUpdate(ac, uuid);
		});
	}

	private void addPublishHandlers() {

		InternalEndpointRoute getEndpoint = createRoute();
		getEndpoint.description("Return the published status of the node.");
		getEndpoint.path("/:nodeUuid/published");
		getEndpoint.addUriParameter("nodeUuid", "Uuid of the node", NODE_DELOREAN_UUID);
		getEndpoint.method(GET);
		getEndpoint.produces(APPLICATION_JSON);
		getEndpoint.exampleResponse(OK, versioningExamples.createPublishStatusResponse(), "Publish status of the node.");
		getEndpoint.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		getEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam("nodeUuid");
			crudHandler.handleGetPublishStatus(ac, uuid);
		});

		InternalEndpointRoute putEndpoint = createRoute();
		putEndpoint.description("Publish all language specific contents of the node with the given uuid.");
		putEndpoint.path("/:nodeUuid/published");
		putEndpoint.addUriParameter("nodeUuid", "Uuid of the node", NODE_DELOREAN_UUID);
		putEndpoint.method(POST);
		putEndpoint.produces(APPLICATION_JSON);
		putEndpoint.exampleResponse(OK, versioningExamples.createPublishStatusResponse(), "Publish status of the node.");
		putEndpoint.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		putEndpoint.addQueryParameters(PublishParametersImpl.class);
		putEndpoint.events(NODE_PUBLISHED);
		putEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam("nodeUuid");
			crudHandler.handlePublish(ac, uuid);
		});

		InternalEndpointRoute deleteEndpoint = createRoute();
		deleteEndpoint.description("Unpublish the given node.");
		deleteEndpoint.path("/:nodeUuid/published");
		deleteEndpoint.addUriParameter("nodeUuid", "Uuid of the node", NODE_DELOREAN_UUID);
		deleteEndpoint.method(DELETE);
		deleteEndpoint.produces(APPLICATION_JSON);
		deleteEndpoint.exampleResponse(NO_CONTENT, "Node was unpublished.");
		deleteEndpoint.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		deleteEndpoint.addQueryParameters(PublishParametersImpl.class);
		deleteEndpoint.events(NODE_UNPUBLISHED);
		deleteEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam("nodeUuid");
			crudHandler.handleTakeOffline(ac, uuid);
		});

		// Language specific

		InternalEndpointRoute getLanguageRoute = createRoute();
		getLanguageRoute.description("Return the publish status for the given language of the node.");
		getLanguageRoute.path("/:nodeUuid/languages/:language/published");
		getLanguageRoute.addUriParameter("nodeUuid", "Uuid of the node", NODE_DELOREAN_UUID);
		getLanguageRoute.addUriParameter("language", "Name of the language tag", "en");
		getLanguageRoute.method(GET);
		getLanguageRoute.produces(APPLICATION_JSON);
		getLanguageRoute.exampleResponse(OK, versioningExamples.createPublishStatusModel(), "Publish status of the specific language.");
		getLanguageRoute.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node or the language could not be found.");
		getLanguageRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam("nodeUuid");
			String lang = rc.request().getParam("language");
			crudHandler.handleGetPublishStatus(ac, uuid, lang);
		});

		InternalEndpointRoute putLanguageRoute = createRoute();
		putLanguageRoute.path("/:nodeUuid/languages/:language/published").method(POST).produces(APPLICATION_JSON);
		putLanguageRoute.addUriParameter("nodeUuid", "Uuid of the node", NODE_DELOREAN_UUID);
		putLanguageRoute.addUriParameter("language", "Name of the language tag", "en");
		putLanguageRoute.description(
			"Publish the language of the node. This will automatically assign a new major version to the node and update the draft version to the published version.");
		putLanguageRoute.exampleResponse(OK, versioningExamples.createPublishStatusModel(), "Updated publish status.");
		putLanguageRoute.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node or the language could not be found.");
		putLanguageRoute.produces(APPLICATION_JSON);
		putLanguageRoute.events(NODE_PUBLISHED);
		putLanguageRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam("nodeUuid");
			String lang = rc.request().getParam("language");
			crudHandler.handlePublish(ac, uuid, lang);
		});

		InternalEndpointRoute deleteLanguageRoute = createRoute();
		deleteLanguageRoute.description("Take the language of the node offline.");
		deleteLanguageRoute.path("/:nodeUuid/languages/:language/published").method(DELETE).produces(APPLICATION_JSON);
		deleteLanguageRoute.addUriParameter("nodeUuid", "Uuid of the node", NODE_DELOREAN_UUID);
		deleteLanguageRoute.addUriParameter("language", "Name of the language tag", "en");
		deleteLanguageRoute.exampleResponse(NO_CONTENT, "Node language was taken offline.");
		deleteLanguageRoute.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node or the language could not be found.");
		deleteLanguageRoute.produces(APPLICATION_JSON);
		deleteLanguageRoute.events(NODE_UNPUBLISHED);
		deleteLanguageRoute.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam("nodeUuid");
			String lang = rc.request().getParam("language");
			crudHandler.handleTakeOffline(ac, uuid, lang);
		});

	}

	public NodeCrudHandler getCrudHandler() {
		return crudHandler;
	}
}
