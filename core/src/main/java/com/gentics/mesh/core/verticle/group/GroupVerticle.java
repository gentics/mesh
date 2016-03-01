package com.gentics.mesh.core.verticle.group;

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
import com.gentics.mesh.core.AbstractCoreApiVerticle;

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
			InternalActionContext ac = InternalActionContext.create(rc);
			String groupUuid = ac.getParameter("groupUuid");
			crudHandler.handleGroupRolesList(ac, groupUuid);
		});

		route("/:groupUuid/roles/:roleUuid").method(PUT).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String groupUuid = ac.getParameter("groupUuid");
			String roleUuid = ac.getParameter("roleUuid");

			crudHandler.handleAddRoleToGroup(ac, groupUuid, roleUuid);
		});

		route("/:groupUuid/roles/:roleUuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String groupUuid = ac.getParameter("groupUuid");
			String roleUuid = ac.getParameter("roleUuid");

			crudHandler.handleRemoveRoleFromGroup(ac, groupUuid, roleUuid);
		});
	}

	private void addGroupUserHandlers() {
		route("/:groupUuid/users").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String groupUuid = ac.getParameter("groupUuid");
			crudHandler.handleGroupUserList(ac, groupUuid);
		});

		Route route = route("/:groupUuid/users/:userUuid").method(PUT).produces(APPLICATION_JSON);
		route.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String groupUuid = ac.getParameter("groupUuid");
			String userUuid = ac.getParameter("userUuid");
			crudHandler.handleAddUserToGroup(ac, groupUuid, userUuid);
		});

		route = route("/:groupUuid/users/:userUuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String groupUuid = ac.getParameter("groupUuid");
			String userUuid = ac.getParameter("userUuid");
			crudHandler.handleRemoveUserFromGroup(ac, groupUuid, userUuid);
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	// TODO Determine what we should do about conflicting group names. Should we let neo4j handle those cases?
	// TODO update timestamps
	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleUpdate(ac, uuid);
		});

	}

	private void addReadHandler() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleRead(ac, uuid);
		});

		/*
		 * List all groups when no parameter was specified
		 */
		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	// TODO handle conflicting group name: group_conflicting_name
	private void addCreateHandler() {
		route("/").method(POST).handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});

	}
}
