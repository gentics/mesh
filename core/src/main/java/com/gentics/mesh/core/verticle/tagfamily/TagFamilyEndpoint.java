package com.gentics.mesh.core.verticle.tagfamily;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.verticle.tag.TagCrudHandler;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractProjectEndpoint;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TagFamilyEndpoint extends AbstractProjectEndpoint {

	private static final Logger log = LoggerFactory.getLogger(TagFamilyEndpoint.class);

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of tag families and tags.";
	}

	private TagFamilyCrudHandler tagFamilyCrudHandler;

	private TagCrudHandler tagCrudHandler;

	public TagFamilyEndpoint() {
		super("tagFamilies", null);
	}

	@Inject
	public TagFamilyEndpoint(BootstrapInitializer boot, TagCrudHandler tagCrudHandler, TagFamilyCrudHandler tagFamilyCrudHandler) {
		super("tagFamilies", boot);
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

		// Tags API
		addTagCreateHandler();
		addTagReadHandler();
		addTagUpdateHandler();
		addTagDeleteHandler();
		addTaggedNodesHandler();

		if (log.isDebugEnabled()) {
			log.debug("Registered tagfamily verticle endpoints");
		}
	}

	// TODO fetch project specific tag
	// TODO update other fields as well?
	// TODO Update user information
	private void addTagUpdateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:tagFamilyUuid/tags/:tagUuid");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		endpoint.addUriParameter("tagUuid", "Uuid of the tag.", UUIDUtil.randomUUID());
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Update the specified tag");
		endpoint.exampleRequest(tagExamples.createTagUpdateRequest("Red"));
		endpoint.exampleResponse(OK, tagExamples.createTagResponse1("Red"), "Updated tag.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			String uuid = ac.getParameter("tagUuid");
			tagCrudHandler.handleUpdate(ac, tagFamilyUuid, uuid);
		});

	}

	// TODO load project specific root tag
	// TODO handle creator
	// TODO maybe projects should not be a set?
	private void addTagCreateHandler() {
		InternalEndpointRoute createTag = createRoute();
		createTag.description("Create a new tag within the tag family.");
		createTag.path("/:tagFamilyUuid/tags").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		createTag.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		createTag.exampleRequest(tagExamples.createTagCreateRequest("red"));
		createTag.exampleResponse(OK, tagExamples.createTagResponse1("red"), "Created tag");
		createTag.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			tagCrudHandler.handleCreate(ac, tagFamilyUuid);
		});
	}

	// TODO filtering, sorting
	private void addTagReadHandler() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:tagFamilyUuid/tags/:tagUuid");
		readOne.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		readOne.addUriParameter("tagUuid", "Uuid of the tag.", UUIDUtil.randomUUID());
		readOne.method(GET);
		readOne.description("Read the specified tag from the tag family.");
		readOne.exampleResponse(OK, tagExamples.createTagResponse1("red"), "Loaded tag.");
		readOne.produces(APPLICATION_JSON);
		readOne.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			String uuid = ac.getParameter("tagUuid");
			tagCrudHandler.handleRead(ac, tagFamilyUuid, uuid);
		});

		InternalEndpointRoute readAll = createRoute();
		readAll.path("/:tagFamilyUuid/tags");
		readAll.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		readAll.method(GET);
		readAll.description("Load tags which were assigned to this tag family and return a paged list response.");
		readAll.exampleResponse(OK, tagExamples.createTagListResponse(), "List of tags.");
		readAll.produces(APPLICATION_JSON);
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			tagCrudHandler.handleReadTagList(ac, tagFamilyUuid);
		});

	}

	// TODO filter by projectName
	private void addTagDeleteHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:tagFamilyUuid/tags/:tagUuid");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		endpoint.addUriParameter("tagUuid", "Uuid of the tag.", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Remove the tag from the tag family.");
		endpoint.exampleResponse(NO_CONTENT, "Tag was removed from the tag family");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			String uuid = ac.getParameter("tagUuid");
			tagCrudHandler.handleDelete(ac, tagFamilyUuid, uuid);
		});
	}

	private void addTaggedNodesHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:tagFamilyUuid/tags/:tagUuid/nodes");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		endpoint.addUriParameter("tagUuid", "Uuid of the tag.", UUIDUtil.randomUUID());
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Load all nodes that have been tagged with the tag and return a paged list response.");
		endpoint.addQueryParameters(PagingParametersImpl.class);
		endpoint.exampleResponse(OK, nodeExamples.getNodeListResponse(), "List of nodes which were tagged using the provided tag.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			String uuid = ac.getParameter("tagUuid");
			tagCrudHandler.handleTaggedNodesList(ac, tagFamilyUuid, uuid);
		});
	}

	private void addTagFamilyDeleteHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:tagFamilyUuid");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Delete the tag family.");
		endpoint.exampleResponse(NO_CONTENT, "Tag family was deleted.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("tagFamilyUuid");
			tagFamilyCrudHandler.handleDelete(ac, uuid);
		});
	}

	private void addTagFamilyReadHandler() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:tagFamilyUuid");
		readOne.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		readOne.method(GET);
		readOne.description("Read the tag family with the given uuid.");
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, tagFamilyExamples.getTagFamilyResponse("Colors"), "Loaded tag family.");
		readOne.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("tagFamilyUuid");
			tagFamilyCrudHandler.handleRead(ac, uuid);
		});

		InternalEndpointRoute readAll = createRoute();
		readAll.path("/");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
		readAll.description("Load multiple tag families and return a paged list response.");
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.exampleResponse(OK, tagFamilyExamples.getTagFamilyListResponse(), "Loaded tag families.");
		readAll.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			tagFamilyCrudHandler.handleReadList(ac);
		});
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
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			tagFamilyCrudHandler.handleCreate(ac);
		});
	}

	private void addTagFamilyUpdateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:tagFamilyUuid");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		endpoint.method(POST);
		endpoint.description("Update the tag family with the given uuid.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(tagFamilyExamples.getTagFamilyUpdateRequest("Nicer colors"));
		endpoint.exampleResponse(OK, tagFamilyExamples.getTagFamilyResponse("Nicer colors"), "Updated tag family.");
		endpoint.handler(rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("tagFamilyUuid");
			tagFamilyCrudHandler.handleUpdate(ac, uuid);
		});
	}
}
