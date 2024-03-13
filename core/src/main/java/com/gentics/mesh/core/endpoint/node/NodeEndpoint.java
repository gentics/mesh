package com.gentics.mesh.core.endpoint.node;

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
import static com.gentics.mesh.core.rest.MeshEvent.S3BINARY_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.S3BINARY_METADATA_EXTRACTED;
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

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.raml.model.Resource;

import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.RolePermissionHandlingProjectEndpoint;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.core.rest.navigation.NavigationResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.parameter.ImageManipulationRetrievalParameters;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.parameter.impl.ImageManipulationRetrievalParametersImpl;
import com.gentics.mesh.parameter.impl.NavigationParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;

import io.vertx.core.MultiMap;

/**
 * The content verticle adds rest endpoints for manipulating nodes.
 */
public class NodeEndpoint extends RolePermissionHandlingProjectEndpoint {

	private Resource resource = new Resource();

	private NodeCrudHandler crudHandler;

	private BinaryUploadHandler binaryUploadHandler;

	private BinaryTransformHandler binaryTransformHandler;

	private BinaryDownloadHandler binaryDownloadHandler;

	private S3BinaryUploadHandlerImpl s3binaryUploadHandler;

	private S3BinaryMetadataExtractionHandlerImpl s3BinaryMetadataExtractionHandler;

	private BinaryVariantsHandler binaryVariantsHandler;

	public NodeEndpoint() {
		super("nodes", null, null, null, null, null);
	}

	@Inject
	public NodeEndpoint(MeshAuthChainImpl chain, BootstrapInitializer boot, NodeCrudHandler crudHandler, BinaryUploadHandler binaryUploadHandler,
		BinaryTransformHandler binaryTransformHandler, BinaryDownloadHandler binaryDownloadHandler, S3BinaryUploadHandlerImpl s3binaryUploadHandler,
						S3BinaryMetadataExtractionHandlerImpl s3BinaryMetadataExtractionHandler, BinaryVariantsHandler binaryVariantsHandler, 
						LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super("nodes", chain, boot, localConfigApi, db, options);
		this.crudHandler = crudHandler;
		this.binaryUploadHandler = binaryUploadHandler;
		this.binaryTransformHandler = binaryTransformHandler;
		this.binaryDownloadHandler = binaryDownloadHandler;
		this.s3binaryUploadHandler = s3binaryUploadHandler;
		this.s3BinaryMetadataExtractionHandler = s3BinaryMetadataExtractionHandler;
		this.binaryVariantsHandler = binaryVariantsHandler;
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
		addBinaryVariantsHandlers();
		addRolePermissionHandler("nodeUuid", NODE_DELOREAN_UUID, "node", crudHandler, true);
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
		}, false);
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
		}, false);
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
		}, isOrderedBlockingHandlers());
	}

	private void addBinaryHandlers() {
		InternalEndpointRoute fieldUpdate = createRoute();
		fieldUpdate.path("/:nodeUuid/binary/:fieldName");
		fieldUpdate.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		fieldUpdate.addUriParameter("fieldName", "Name of the field which should be created.", "stringField");
		fieldUpdate.addUriParameter("publish", "Whether the node shall be published after updating the binary field", "true");
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
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute checkCallback = createRoute();
		checkCallback.path("/:nodeUuid/binary/:fieldName/checkCallback");
		checkCallback.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		checkCallback.addUriParameter("fieldName", "Name of the field for which the check status is to be updated.", "stringField");
		checkCallback.method(POST);
		checkCallback.produces(APPLICATION_JSON);
		checkCallback.exampleRequest(nodeExamples.getExampleBinaryCheckCallbackParameters());
		checkCallback.exampleResponse(NO_CONTENT, "");
		checkCallback.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node or the field could not be found.");
		checkCallback.description("Set the check status for the binaryfield with the given name.");
		checkCallback.events(NODE_UPDATED);
		checkCallback.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			InternalActionContext ac = wrap(rc);
			binaryUploadHandler.handleBinaryCheckResult(ac, uuid, fieldName);
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
		}, isOrderedBlockingHandlers());

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
		}, false);
	}

	private void addBinaryVariantsHandlers() {
		InternalEndpointRoute fieldGet = createRoute();
		fieldGet.path("/:nodeUuid/binary/:fieldName/variants");
		fieldGet.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		fieldGet.addUriParameter("fieldName", "Name of the image field", "image");
		fieldGet.addQueryParameters(VersioningParametersImpl.class);
		fieldGet.addQueryParameters(ImageManipulationRetrievalParametersImpl.class);
		fieldGet.produces(APPLICATION_JSON);
		fieldGet.exampleResponse(OK, nodeExamples.createImageVariantsResponse(), "A list of image variants have been returned.");
		fieldGet.method(GET);
		fieldGet.description(
			"Get the list of image manipulation variants of the binary, possessed by a field with the given name.");
		fieldGet.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			binaryVariantsHandler.handleListBinaryFieldVariants(wrap(rc), uuid, fieldName);
		}, false);

		InternalEndpointRoute fieldDelete = createRoute();
		fieldDelete.path("/:nodeUuid/binary/:fieldName/variants");
		fieldDelete.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		fieldDelete.addUriParameter("fieldName", "Name of the image field", "image");
		fieldDelete.addQueryParameters(VersioningParametersImpl.class);
		fieldDelete.addQueryParameters(ImageManipulationRetrievalParametersImpl.class);
		fieldDelete.produces(APPLICATION_JSON);
		fieldDelete.method(DELETE);
		fieldDelete.exampleResponse(NO_CONTENT, "Image variants have been deleted.");
		fieldDelete.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		fieldDelete.description(
			"Delete unused image manipulation variants of the binary, referenced by a field with the given name.");
		fieldDelete.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			binaryVariantsHandler.handleDeleteBinaryFieldVariants(wrap(rc), uuid, fieldName);
		}, false);

		InternalEndpointRoute fieldPut = createRoute();
		fieldPut.path("/:nodeUuid/binary/:fieldName/variants");
		fieldPut.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		fieldPut.addUriParameter("fieldName", "Name of the image field.", "image");
		fieldPut.addQueryParameters(VersioningParametersImpl.class);
		fieldPut.addQueryParameters(ImageManipulationRetrievalParametersImpl.class);
		fieldPut.method(POST);
		fieldPut.produces(APPLICATION_JSON);
		fieldPut.exampleRequest(nodeExamples.createImageManipulationRequest());
		fieldPut.exampleResponse(OK, nodeExamples.createImageVariantsResponse(), "An updated list of variants is returned");
		fieldPut.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node or the field could not be found.");
		fieldPut.description("Add new image variants to the binary, referenced by a field with the given name.");
		fieldPut.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			binaryVariantsHandler.handleUpsertBinaryFieldVariants(wrap(rc), uuid, fieldName);
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
		fieldUpdate.description("Create the s3 binaryfield with the given name.");
		fieldUpdate.events(NODE_UPDATED, S3BINARY_CREATED);
		fieldUpdate.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			InternalActionContext ac = wrap(rc);
			s3binaryUploadHandler.handleUpdateField(ac, uuid, fieldName);
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute checkCallback = createRoute();
		checkCallback.path("/:nodeUuid/s3binary/:fieldName/checkCallback");
		checkCallback.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		checkCallback.addUriParameter("fieldName", "Name of the field which should be created.", "stringField");
		checkCallback.method(POST);
		checkCallback.produces(APPLICATION_JSON);
		checkCallback.exampleRequest(nodeExamples.getExampleBinaryCheckCallbackParameters());
		checkCallback.exampleResponse(NO_CONTENT, "");
		checkCallback.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node or the field could not be found.");
		checkCallback.description("Set the check status for the binaryfield with the given name.");
		checkCallback.events(NODE_UPDATED);
		checkCallback.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			InternalActionContext ac = wrap(rc);

			s3binaryUploadHandler.handleBinaryCheckResult(ac, uuid, fieldName);
		});

		InternalEndpointRoute fieldMetadataExtraction = createRoute();
		fieldMetadataExtraction.path("/:nodeUuid/s3binary/:fieldName/parseMetadata");
		fieldMetadataExtraction.addUriParameter("nodeUuid", "Uuid of the node.", NODE_DELOREAN_UUID);
		fieldMetadataExtraction.addUriParameter("fieldName", "Name of the field which should be created.", "stringField");
		fieldMetadataExtraction.method(POST);
		fieldMetadataExtraction.produces(APPLICATION_JSON);
		fieldMetadataExtraction.exampleRequest(nodeExamples.getExampleBinaryUploadFormParameters());
		fieldMetadataExtraction.exampleResponse(OK, nodeExamples.getNodeResponseWithAllFields(), "The response contains the updated node.");
		fieldMetadataExtraction.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node or the field could not be found.");
		fieldMetadataExtraction.description("Parse metadata of s3 binaryfield with the given name.");
		fieldMetadataExtraction.events(S3BINARY_METADATA_EXTRACTED);
		fieldMetadataExtraction.blockingHandler(rc -> {
			String uuid = rc.request().getParam("nodeUuid");
			String fieldName = rc.request().getParam("fieldName");
			InternalActionContext ac = wrap(rc);
			s3BinaryMetadataExtractionHandler.handleMetadataExtraction(ac, uuid, fieldName);
		}, isOrderedBlockingHandlers());
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
		}, isOrderedBlockingHandlers());
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
		}, false);
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
		}, false);

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
		}, isOrderedBlockingHandlers());

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
		}, isOrderedBlockingHandlers());

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
		}, isOrderedBlockingHandlers());

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
		}, isOrderedBlockingHandlers());
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
		}, false);

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
		}, false);

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
		}, isOrderedBlockingHandlers());
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
		}, isOrderedBlockingHandlers());
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
		}, false);

		InternalEndpointRoute postEndpoint = createRoute();
		postEndpoint.description("Publish all language specific contents of the node with the given uuid.");
		postEndpoint.path("/:nodeUuid/published");
		postEndpoint.addUriParameter("nodeUuid", "Uuid of the node", NODE_DELOREAN_UUID);
		postEndpoint.method(POST);
		postEndpoint.produces(APPLICATION_JSON);
		postEndpoint.exampleResponse(OK, versioningExamples.createPublishStatusResponse(), "Publish status of the node.");
		postEndpoint.exampleResponse(NOT_FOUND, miscExamples.createMessageResponse(), "The node could not be found.");
		postEndpoint.addQueryParameters(PublishParametersImpl.class);
		postEndpoint.events(NODE_PUBLISHED);
		postEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = rc.request().getParam("nodeUuid");
			crudHandler.handlePublish(ac, uuid);
		}, isOrderedBlockingHandlers());

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
		}, isOrderedBlockingHandlers());

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
		}, false);

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
		}, isOrderedBlockingHandlers());

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
		}, isOrderedBlockingHandlers());

	}

	public NodeCrudHandler getCrudHandler() {
		return crudHandler;
	}
}
