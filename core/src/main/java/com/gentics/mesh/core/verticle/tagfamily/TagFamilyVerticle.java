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
import com.gentics.mesh.rest.Endpoint;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;

@Component
@Scope("singleton")
@SpringVerticle
public class TagFamilyVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(TagFamilyVerticle.class);

	@Autowired
	private TagFamilyCrudHandler tagFamilyCrudHandler;

	@Autowired
	private TagCrudHandler tagCrudHandler;

	public TagFamilyVerticle() {
		super("tagFamilies");
	}

	@Override
	public void registerEndPoints() throws Exception {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/*");
		endpoint.handler(getSpringConfiguration().authHandler());

		getRouter().routeWithRegex("\\/([^\\/]{32})\\/.*").handler(tagFamilyCrudHandler.getUuidHandler("tagfamily_not_found"));

		addTagFamilyReadHandler();
		addTagFamilyCreateHandler();
		addTagFamilyUpdateHandler();
		addTagFamilyDeleteHandler();

		//Tags API
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
		Endpoint addTag = createEndpoint();
		addTag.path("/:tagFamilyUuid/tags/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		addTag.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			String uuid = ac.getParameter("uuid");
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
		createTag.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			tagCrudHandler.handleCreate(ac, tagFamilyUuid);
		});
	}

	// TODO filtering, sorting
	private void addTagReadHandler() {
		Route route = route("/:tagFamilyUuid/tags/:uuid").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			String uuid = ac.getParameter("uuid");
			tagCrudHandler.handleRead(ac, tagFamilyUuid, uuid);
		});

		Route readAllRoute = route("/:tagFamilyUuid/tags").method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			tagCrudHandler.handleReadTagList(ac, tagFamilyUuid);
		});

	}

	// TODO filter by projectName
	private void addTagDeleteHandler() {
		Route route = route("/:tagFamilyUuid/tags/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			String uuid = ac.getParameter("uuid");
			tagCrudHandler.handleDelete(ac, tagFamilyUuid, uuid);
		});
	}

	private void addTaggedNodesHandler() {
		Endpoint listTaggedNodes = createEndpoint();
		listTaggedNodes.path("/:tagFamilyUuid/tags/:uuid/nodes").method(GET).produces(APPLICATION_JSON);
		listTaggedNodes.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String tagFamilyUuid = ac.getParameter("tagFamilyUuid");
			String uuid = ac.getParameter("uuid");
			tagCrudHandler.handleTaggedNodesList(ac, tagFamilyUuid, uuid);
		});
	}

	private void addTagFamilyDeleteHandler() {
		Endpoint deleteTagFamily = createEndpoint();
		deleteTagFamily.path("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		deleteTagFamily.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			tagFamilyCrudHandler.handleDelete(ac, uuid);
		});
	}

	private void addTagFamilyReadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:uuid");
		readOne.method(GET);
		readOne.description("Read the tag family with the given uuid.");
		readOne.produces(APPLICATION_JSON);
		readOne.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			tagFamilyCrudHandler.handleRead(ac, uuid);
		});

		Endpoint readAll = createEndpoint();
		readAll.path("/");
		readAll.method(GET);
		readAll.produces(APPLICATION_JSON);
		readAll.description("Load multiple tag families and return a paged list response.");
		readAll.handler(rc -> {
			tagFamilyCrudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	private void addTagFamilyCreateHandler() {
		Endpoint createTagFamily = createEndpoint();
		createTagFamily.path("/");
		createTagFamily.method(POST);
		createTagFamily.description("Create a new tag family.");
		createTagFamily.consumes(APPLICATION_JSON);
		createTagFamily.produces(APPLICATION_JSON);
		createTagFamily.handler(rc -> {
			tagFamilyCrudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}

	private void addTagFamilyUpdateHandler() {
		Endpoint updateTagFamily = createEndpoint();
		updateTagFamily.path("/:uuid");
		updateTagFamily.method(PUT);
		updateTagFamily.description("Update the tagfamily with the given uuid.");
		updateTagFamily.consumes(APPLICATION_JSON);
		updateTagFamily.produces(APPLICATION_JSON);
		updateTagFamily.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			tagFamilyCrudHandler.handleUpdate(ac, uuid);
		});
	}
}
