package com.gentics.mesh.core.verticle;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.verticle.handler.GroupCrudHandler;

import io.vertx.ext.web.Route;

@Component
@Scope("singleton")
@SpringVerticle
public class GroupVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private GroupCrudHandler crudHandler;

	public GroupVerticle() {
		super("groups");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addGroupUserHandlers();
		addGroupRoleHandlers();

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addGroupRoleHandlers() {

		route("/:groupUuid/roles").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleGroupRolesList(rc);
		});

		route("/:groupUuid/roles/:roleUuid").method(POST).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleAddRoleToGroup(rc);
		});

		route("/:groupUuid/roles/:roleUuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleRemoveRoleFromGroup(rc);
		});
	}

	private void addGroupUserHandlers() {
		route("/:groupUuid/users").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleGroupUserList(rc);
		});

		Route route = route("/:groupUuid/users/:userUuid").method(POST).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleAddUserToGroup(rc);
		});

		route = route("/:groupUuid/users/:userUuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			crudHandler.handleRemoveUserFromGroup(rc);
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleDelete(rc);
		});
	}

	// TODO Determine what we should do about conflicting group names. Should we let neo4j handle those cases?
	// TODO update timestamps
	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleUpdate(rc);
		});

	}

	private void addReadHandler() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleRead(rc);
		});

		/*
		 * List all groups when no parameter was specified
		 */
		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleReadList(rc);
		});
	}

	// TODO handle conflicting group name: group_conflicting_name
	private void addCreateHandler() {
		route("/").method(POST).handler(rc -> {
			crudHandler.handleCreate(rc);
		});

	}
}
