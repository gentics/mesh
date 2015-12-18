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

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.verticle.tag.TagCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

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
		route("/*").handler(springConfiguration.authHandler());
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
		Route route = route("/:tagFamilyUuid/tags/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			tagCrudHandler.handleUpdate(InternalActionContext.create(rc));
		});

	}

	// TODO load project specific root tag
	// TODO handle creator
	// TODO maybe projects should not be a set?
	private void addTagCreateHandler() {
		Route route = route("/:tagFamilyUuid/tags").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			tagCrudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}

	// TODO filtering, sorting
	private void addTagReadHandler() {
		Route route = route("/:tagFamilyUuid/tags/:uuid").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			tagCrudHandler.handleRead(InternalActionContext.create(rc));
		});

		Route readAllRoute = route("/:tagFamilyUuid/tags").method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			tagCrudHandler.handleReadList(InternalActionContext.create(rc));
		});

	}

	// TODO filter by projectName
	private void addTagDeleteHandler() {
		Route route = route("/:tagFamilyUuid/tags/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			tagCrudHandler.handleDelete(InternalActionContext.create(rc));
		});
	}

	private void addTaggedNodesHandler() {
		Route getRoute = route("/:tagFamilyUuid/tags/:uuid/nodes").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			tagCrudHandler.handleTaggedNodesList(InternalActionContext.create(rc));
		});
	}

	private void addTagFamilyDeleteHandler() {
		Route deleteRoute = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			tagFamilyCrudHandler.handleDelete(InternalActionContext.create(rc));
		});
	}

	private void addTagFamilyReadHandler() {
		Route readRoute = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		readRoute.handler(rc -> {
			tagFamilyCrudHandler.handleRead(InternalActionContext.create(rc));
		});

		Route readAllRoute = route("/").method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			tagFamilyCrudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	private void addTagFamilyCreateHandler() {
		Route createRoute = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		createRoute.handler(rc -> {
			tagFamilyCrudHandler.handleCreate(InternalActionContext.create(rc));
		});
	}

	private void addTagFamilyUpdateHandler() {
		Route updateRoute = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		updateRoute.handler(rc -> {
			tagFamilyCrudHandler.handleUpdate(InternalActionContext.create(rc));
		});
	}
}
