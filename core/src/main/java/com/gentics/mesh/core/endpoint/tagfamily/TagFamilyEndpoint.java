package com.gentics.mesh.core.endpoint.tagfamily;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNTAGGED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_PERMISSIONS_CHANGED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_UPDATED;
import static com.gentics.mesh.example.ExampleUuids.TAGFAMILY_COLORS_UUID;
import static com.gentics.mesh.example.ExampleUuids.TAG_BLUE_UUID;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.PathParameters;
import com.gentics.mesh.core.endpoint.RolePermissionHandlingProjectEndpoint;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.core.endpoint.tag.TagCrudHandler;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;

/**
 * Endpoint for /api/v1/:project/tagFamilies
 */
public class TagFamilyEndpoint extends RolePermissionHandlingProjectEndpoint {

	private static final Logger log = LoggerFactory.getLogger(TagFamilyEndpoint.class);

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of tag families and tags.";
	}

	private TagFamilyCrudHandler tagFamilyCrudHandler;

	private TagCrudHandler tagCrudHandler;

	public TagFamilyEndpoint() {
		super("tagFamilies", null, null, null, null, null);
	}

	@Inject
	public TagFamilyEndpoint(MeshAuthChain chain, BootstrapInitializer boot, TagCrudHandler tagCrudHandler,
		TagFamilyCrudHandler tagFamilyCrudHandler, LocalConfigApi localConfigApi, Database db, MeshOptions options) {
		super("tagFamilies", chain, boot, localConfigApi, db, options);
		this.tagCrudHandler = tagCrudHandler;
		this.tagFamilyCrudHandler = tagFamilyCrudHandler;
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		if (tagFamilyCrudHandler != null) {
			getRouter().routeWithRegex("\\/([^\\/]{32})\\/.*").handler(tagFamilyCrudHandler.getUuidHandler("tagfamily_not_found"));
		}

		addTagFamilyReadHandler();
		addTagFamilyCreateHandler();
		addTagFamilyUpdateHandler();
		addTagFamilyDeleteHandler();
		addRolePermissionHandler("tagFamilyUuid", TAGFAMILY_COLORS_UUID, "tag family", tagFamilyCrudHandler, false);

		// Tags API
		addTagCreateHandler();
		addTagReadHandler();
		addTagUpdateHandler();
		addTagDeleteHandler();
		addTaggedNodesHandler();
		addTagRolePermissionHandler();

		if (log.isDebugEnabled()) {
			log.debug("Registered tagfamily verticle endpoints");
		}
	}

	private void addTagUpdateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:tagFamilyUuid/tags/:tagUuid");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", TAGFAMILY_COLORS_UUID);
		endpoint.addUriParameter("tagUuid", "Uuid of the tag.", TAG_BLUE_UUID);
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Update the specified tag. The tag is created if no tag with the specified uuid could be found.");
		endpoint.exampleRequest(tagExamples.createTagUpdateRequest("Red"));
		endpoint.exampleResponse(OK, tagExamples.createTagResponse1("Red"), "Updated tag.");
		endpoint.events(TAG_UPDATED, TAG_CREATED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tagFamilyUuid = PathParameters.getTagFamilyUuid(rc);
			String uuid = PathParameters.getTagUuid(rc);
			tagCrudHandler.handleUpdate(ac, tagFamilyUuid, uuid);
		}, isOrderedBlockingHandlers());

	}

	private void addTagCreateHandler() {
		InternalEndpointRoute createTag = createRoute();
		createTag.description("Create a new tag within the tag family.");
		createTag.path("/:tagFamilyUuid/tags").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		createTag.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", TAGFAMILY_COLORS_UUID);
		createTag.exampleRequest(tagExamples.createTagCreateRequest("red"));
		createTag.exampleResponse(OK, tagExamples.createTagResponse1("red"), "Created tag");
		createTag.events(TAG_CREATED);
		createTag.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tagFamilyUuid = PathParameters.getTagFamilyUuid(rc);
			tagCrudHandler.handleCreate(ac, tagFamilyUuid);
		}, isOrderedBlockingHandlers());
	}

	private void addTagReadHandler() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:tagFamilyUuid/tags/:tagUuid");
		readOne.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", TAGFAMILY_COLORS_UUID);
		readOne.addUriParameter("tagUuid", "Uuid of the tag.", TAG_BLUE_UUID);
		readOne.method(GET);
		readOne.description("Read the specified tag from the tag family.");
		readOne.exampleResponse(OK, tagExamples.createTagResponse1("red"), "Loaded tag.");
		readOne.addQueryParameters(GenericParametersImpl.class);
		readOne.produces(APPLICATION_JSON);
		readOne.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tagFamilyUuid = PathParameters.getTagFamilyUuid(rc);
			String uuid = PathParameters.getTagUuid(rc);
			tagCrudHandler.handleRead(ac, tagFamilyUuid, uuid);
		}, false);

		InternalEndpointRoute readAll = createRoute();
		readAll.path("/:tagFamilyUuid/tags");
		readAll.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", TAGFAMILY_COLORS_UUID);
		readAll.method(GET);
		readAll.description("Load tags which were assigned to this tag family and return a paged list response.");
		readAll.exampleResponse(OK, tagExamples.createTagListResponse(), "List of tags.");
		readAll.produces(APPLICATION_JSON);
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.addQueryParameters(GenericParametersImpl.class);
		readAll.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tagFamilyUuid = PathParameters.getTagFamilyUuid(rc);
			tagCrudHandler.handleReadTagList(ac, tagFamilyUuid);
		}, false);

	}

	// TODO filter by projectName
	private void addTagDeleteHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:tagFamilyUuid/tags/:tagUuid");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", TAGFAMILY_COLORS_UUID);
		endpoint.addUriParameter("tagUuid", "Uuid of the tag.", TAG_BLUE_UUID);
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Remove the tag from the tag family.");
		endpoint.exampleResponse(NO_CONTENT, "Tag was removed from the tag family");
		endpoint.events(TAG_DELETED, NODE_UNTAGGED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tagFamilyUuid = PathParameters.getTagFamilyUuid(rc);
			String uuid = PathParameters.getTagUuid(rc);
			tagCrudHandler.handleDelete(ac, tagFamilyUuid, uuid);
		}, isOrderedBlockingHandlers());
	}

	private void addTagRolePermissionHandler() {
		InternalEndpointRoute readPermissionsEndpoint = createRoute();
		readPermissionsEndpoint.path("/:tagFamilyUuid/tags/:tagUuid/rolePermissions");
		readPermissionsEndpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", TAGFAMILY_COLORS_UUID);
		readPermissionsEndpoint.addUriParameter("tagUuid", "Uuid of the tag.", TAG_BLUE_UUID);
		readPermissionsEndpoint.method(GET);
		readPermissionsEndpoint.description("Get the permissions on the tag for all roles.");
		readPermissionsEndpoint.produces(APPLICATION_JSON);
		readPermissionsEndpoint.exampleResponse(OK, roleExamples.getObjectPermissionResponse(false), "Loaded permissions.");
		readPermissionsEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tagFamilyUuid = PathParameters.getTagFamilyUuid(rc);
			String uuid = PathParameters.getTagUuid(rc);
			tagCrudHandler.handleReadPermissions(ac, tagFamilyUuid, uuid);
		}, false);

		InternalEndpointRoute grantPermissionsEndpoint = createRoute();
		grantPermissionsEndpoint.path("/:tagFamilyUuid/tags/:tagUuid/rolePermissions");
		grantPermissionsEndpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", TAGFAMILY_COLORS_UUID);
		grantPermissionsEndpoint.addUriParameter("tagUuid", "Uuid of the tag.", TAG_BLUE_UUID);
		grantPermissionsEndpoint.method(POST);
		grantPermissionsEndpoint.description("Grant permissions on the tag to multiple roles.");
		grantPermissionsEndpoint.consumes(APPLICATION_JSON);
		grantPermissionsEndpoint.produces(APPLICATION_JSON);
		grantPermissionsEndpoint.exampleRequest(roleExamples.getObjectPermissionGrantRequest(false));
		grantPermissionsEndpoint.exampleResponse(OK, roleExamples.getObjectPermissionResponse(false), "Updated permissions.");
		grantPermissionsEndpoint.events(ROLE_PERMISSIONS_CHANGED);
		grantPermissionsEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tagFamilyUuid = PathParameters.getTagFamilyUuid(rc);
			String uuid = PathParameters.getTagUuid(rc);
			tagCrudHandler.handleGrantPermissions(ac, tagFamilyUuid, uuid);
		}, isOrderedBlockingHandlers());

		InternalEndpointRoute revokePermissionsEndpoint = createRoute();
		revokePermissionsEndpoint.path("/:tagFamilyUuid/tags/:tagUuid/rolePermissions");
		revokePermissionsEndpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", TAGFAMILY_COLORS_UUID);
		revokePermissionsEndpoint.addUriParameter("tagUuid", "Uuid of the tag.", TAG_BLUE_UUID);
		revokePermissionsEndpoint.method(DELETE);
		revokePermissionsEndpoint.description("Revoke permissions on the tag from multiple roles.");
		revokePermissionsEndpoint.consumes(APPLICATION_JSON);
		revokePermissionsEndpoint.produces(APPLICATION_JSON);
		revokePermissionsEndpoint.exampleRequest(roleExamples.getObjectPermissionRevokeRequest(false));
		revokePermissionsEndpoint.exampleResponse(OK, roleExamples.getObjectPermissionResponse(false), "Updated permissions.");
		revokePermissionsEndpoint.events(ROLE_PERMISSIONS_CHANGED);
		revokePermissionsEndpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tagFamilyUuid = PathParameters.getTagFamilyUuid(rc);
			String uuid = PathParameters.getTagUuid(rc);
			tagCrudHandler.handleRevokePermissions(ac, tagFamilyUuid, uuid);
		}, isOrderedBlockingHandlers());
	}

	private void addTaggedNodesHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:tagFamilyUuid/tags/:tagUuid/nodes");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", TAGFAMILY_COLORS_UUID);
		endpoint.addUriParameter("tagUuid", "Uuid of the tag.", TAG_BLUE_UUID);
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Load all nodes that have been tagged with the tag and return a paged list response.");
		endpoint.addQueryParameters(PagingParametersImpl.class);
		endpoint.exampleResponse(OK, nodeExamples.getNodeListResponse(), "List of nodes which were tagged using the provided tag.");
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tagFamilyUuid = PathParameters.getTagFamilyUuid(rc);
			String uuid = PathParameters.getTagUuid(rc);
			tagCrudHandler.handleTaggedNodesList(ac, tagFamilyUuid, uuid);
		}, false);
	}

	private void addTagFamilyDeleteHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:tagFamilyUuid");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", TAGFAMILY_COLORS_UUID);
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Delete the tag family.");
		endpoint.exampleResponse(NO_CONTENT, "Tag family was deleted.");
		endpoint.events(TAG_FAMILY_DELETED, TAG_DELETED, NODE_UNTAGGED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tagFamilyUuid = PathParameters.getTagFamilyUuid(rc);
			tagFamilyCrudHandler.handleDelete(ac, tagFamilyUuid);
		}, isOrderedBlockingHandlers());
	}

	private void addTagFamilyReadHandler() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:tagFamilyUuid");
		readOne.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", TAGFAMILY_COLORS_UUID);
		readOne.method(GET);
		readOne.description("Read the tag family with the given uuid.");
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, tagFamilyExamples.getTagFamilyResponse("Colors"), "Loaded tag family.");
		readOne.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tagFamilyUuid = PathParameters.getTagFamilyUuid(rc);
			tagFamilyCrudHandler.handleRead(ac, tagFamilyUuid);
		}, false);

		InternalEndpointRoute readAll = createRoute();
		readAll.path("/");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
		readAll.description("Load multiple tag families and return a paged list response.");
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.exampleResponse(OK, tagFamilyExamples.getTagFamilyListResponse(), "Loaded tag families.");
		readAll.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			tagFamilyCrudHandler.handleReadList(ac);
		}, false);
	}

	private void addTagFamilyCreateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new tag family.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(tagFamilyExamples.getTagFamilyCreateRequest("Colors"));
		endpoint.exampleResponse(CREATED, tagFamilyExamples.getTagFamilyResponse("Colors"), "Created tag family.");
		endpoint.events(TAG_FAMILY_CREATED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			tagFamilyCrudHandler.handleCreate(ac);
		}, isOrderedBlockingHandlers());
	}

	private void addTagFamilyUpdateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:tagFamilyUuid");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", TAGFAMILY_COLORS_UUID);
		endpoint.method(POST);
		endpoint.description("Update the tag family with the given uuid. The tag family will be created if it can't be found for the given uuid.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(tagFamilyExamples.getTagFamilyUpdateRequest("Nicer colors"));
		endpoint.exampleResponse(OK, tagFamilyExamples.getTagFamilyResponse("Nicer colors"), "Updated tag family.");
		endpoint.events(TAG_FAMILY_UPDATED, TAG_FAMILY_CREATED);
		endpoint.blockingHandler(rc -> {
			InternalActionContext ac = wrap(rc);
			String tagFamilyUuid = PathParameters.getTagFamilyUuid(rc);
			tagFamilyCrudHandler.handleUpdate(ac, tagFamilyUuid);
		}, isOrderedBlockingHandlers());
	}
}
