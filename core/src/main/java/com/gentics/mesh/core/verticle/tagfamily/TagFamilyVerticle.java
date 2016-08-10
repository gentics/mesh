package com.gentics.mesh.core.verticle.tagfamily;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.verticle.tag.TagCrudHandler;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
@Scope("singleton")
@SpringVerticle
public class TagFamilyVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(TagFamilyVerticle.class);

	@Autowired
	private TagFamilyCrudHandler tagFamilyCrudHandler;

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of tag families and tags.";
	}

	@Autowired
	private TagCrudHandler tagCrudHandler;

	public TagFamilyVerticle() {
		super("tagFamilies");
	}

	@Override
	public void registerEndPoints() throws Exception {
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
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:tagFamilyUuid/tags/:tagUuid");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		endpoint.addUriParameter("tagUuid", "Uuid of the tag.", UUIDUtil.randomUUID());
		endpoint.method(PUT);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Update the specified tag");
		endpoint.exampleRequest(tagExamples.getTagUpdateRequest("Red"));
		endpoint.exampleResponse(200, tagExamples.getTagResponse1("Red"));
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			String uuid = ac.getParameter("tagUuid");
			tagCrudHandler.handleUpdate(ac, tagFamilyUuid, uuid);
		});

	}

	// TODO load project specific root tag
	// TODO handle creator
	// TODO maybe projects should not be a set?
	private void addTagCreateHandler() {
		Endpoint createTag = createEndpoint();
		createTag.description("Create a new tag within the tag family.");
		createTag.path("/:tagFamilyUuid/tags").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		createTag.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		createTag.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			tagCrudHandler.handleCreate(ac, tagFamilyUuid);
		});
	}

	// TODO filtering, sorting
	private void addTagReadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:tagFamilyUuid/tags/:tagUuid");
		readOne.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		readOne.addUriParameter("tagUuid", "Uuid of the tag.", UUIDUtil.randomUUID());
		readOne.method(GET);
		readOne.description("Read the specified tag from the tag family.");
		readOne.exampleResponse(200, tagExamples.getTagResponse1("red"));
		readOne.produces(APPLICATION_JSON);
		readOne.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			String uuid = ac.getParameter("tagUuid");
			tagCrudHandler.handleRead(ac, tagFamilyUuid, uuid);
		});

		Endpoint readAll = createEndpoint();
		readAll.path("/:tagFamilyUuid/tags");
		readAll.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		readAll.method(GET);
		readAll.description("Load tags which were assigned to this tag family and return a paged list response.");
		readAll.exampleResponse(200, tagExamples.getTagListResponse());
		readAll.produces(APPLICATION_JSON);
		readAll.addQueryParameters(PagingParameters.class);
		readAll.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			tagCrudHandler.handleReadTagList(ac, tagFamilyUuid);
		});

	}

	// TODO filter by projectName
	private void addTagDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:tagFamilyUuid/tags/:tagUuid");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		endpoint.addUriParameter("tagUuid", "Uuid of the tag.", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Remove the tag from the tag family.");
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			String uuid = ac.getParameter("tagUuid");
			tagCrudHandler.handleDelete(ac, tagFamilyUuid, uuid);
		});
	}

	private void addTaggedNodesHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:tagFamilyUuid/tags/:tagUuid/nodes");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		endpoint.addUriParameter("tagUuid", "Uuid of the tag.", UUIDUtil.randomUUID());
		endpoint.method(GET);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Load all nodes that have been tagged with the tag and return a paged list response.");
		endpoint.addQueryParameters(PagingParameters.class);
		endpoint.exampleResponse(200, nodeExamples.getNodeListResponse());
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			String uuid = ac.getParameter("tagUuid");
			tagCrudHandler.handleTaggedNodesList(ac, tagFamilyUuid, uuid);
		});
	}

	private void addTagFamilyDeleteHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:tagFamilyUuid");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		endpoint.method(DELETE);
		endpoint.produces(APPLICATION_JSON);
		endpoint.description("Delete the tag family.");
		endpoint.exampleResponse(200, miscExamples.getMessageResponse());
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("tagFamilyUuid");
			tagFamilyCrudHandler.handleDelete(ac, uuid);
		});
	}

	private void addTagFamilyReadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:tagFamilyUuid");
		readOne.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		readOne.method(GET);
		readOne.description("Read the tag family with the given uuid.");
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(200, tagFamilyExamples.getTagFamilyResponse("Colors"));
		readOne.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("tagFamilyUuid");
			tagFamilyCrudHandler.handleRead(ac, uuid);
		});

		Endpoint readAll = createEndpoint();
		readAll.path("/");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
		readAll.description("Load multiple tag families and return a paged list response.");
		readAll.addQueryParameters(PagingParameters.class);
		readAll.exampleResponse(200, tagFamilyExamples.getTagFamilyListResponse());
		readAll.handler(rc -> {
			tagFamilyCrudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	private void addTagFamilyCreateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new tag family.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(tagFamilyExamples.getTagFamilyCreateRequest("Colors"));
		endpoint.exampleResponse(201, tagFamilyExamples.getTagFamilyResponse("Colors"));
		endpoint.handler(rc -> {
			tagFamilyCrudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}

	private void addTagFamilyUpdateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:tagFamilyUuid");
		endpoint.addUriParameter("tagFamilyUuid", "Uuid of the tag family.", UUIDUtil.randomUUID());
		endpoint.method(PUT);
		endpoint.description("Update the tag family with the given uuid.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(tagFamilyExamples.getTagFamilyUpdateRequest("Nicer colors"));
		endpoint.exampleResponse(200, tagFamilyExamples.getTagFamilyResponse("Nicer colors"));
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("tagFamilyUuid");
			tagFamilyCrudHandler.handleUpdate(ac, uuid);
		});
	}
}
