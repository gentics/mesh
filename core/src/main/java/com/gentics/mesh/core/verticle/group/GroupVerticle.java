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
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.rest.Endpoint;

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
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of groups.";
	}

	@Override
	public void registerEndPoints() throws Exception {
		secureAll();

		addGroupUserHandlers();
		addGroupRoleHandlers();

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addGroupRoleHandlers() {
		Endpoint readRoles = createEndpoint();
		readRoles.path("/:groupUuid/roles");
		readRoles.method(GET);
		readRoles.description("Load multiple roles that are assigned to the group. Return a paged list response.");
		readRoles.produces(APPLICATION_JSON);
		readRoles.exampleResponse(200, roleExamples.getRoleListResponse());
		readRoles.addQueryParameters(PagingParameters.class);
		readRoles.addQueryParameters(RolePermissionParameters.class);
		readRoles.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String groupUuid = ac.getParameter("groupUuid");
			crudHandler.handleGroupRolesList(ac, groupUuid);
		});

		Endpoint addRole = createEndpoint();
		addRole.path("/:groupUuid/roles/:roleUuid");
		addRole.method(PUT);
		addRole.description("Add the specified role to the group.");
		addRole.produces(APPLICATION_JSON);
		addRole.exampleResponse(200, groupExamples.getGroupResponse1("Group name"));
		addRole.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String groupUuid = ac.getParameter("groupUuid");
			String roleUuid = ac.getParameter("roleUuid");
			crudHandler.handleAddRoleToGroup(ac, groupUuid, roleUuid);
		});

		Endpoint removeRole = createEndpoint();
		removeRole.path("/:groupUuid/roles/:roleUuid");
		removeRole.method(DELETE);
		removeRole.description("Remove the given role from the group.");
		removeRole.exampleResponse(200, groupExamples.getGroupResponse1("Group name"));
		removeRole.produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String groupUuid = ac.getParameter("groupUuid");
			String roleUuid = ac.getParameter("roleUuid");
			crudHandler.handleRemoveRoleFromGroup(ac, groupUuid, roleUuid);
		});
	}

	private void addGroupUserHandlers() {
		Endpoint readUsers = createEndpoint();
		readUsers.path("/:groupUuid/users");
		readUsers.method(GET);
		readUsers.produces(APPLICATION_JSON);
		readUsers.exampleResponse(200, userExamples.getUserListResponse());
		readUsers.description("Load a list of users which have been assigned to the group");
		readUsers.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String groupUuid = ac.getParameter("groupUuid");
			crudHandler.handleGroupUserList(ac, groupUuid);
		});

		Endpoint addUser = createEndpoint();
		addUser.path("/:groupUuid/users/:userUuid");
		addUser.method(PUT);
		addUser.description("Add the given user to the group");
		addUser.produces(APPLICATION_JSON);
		addUser.exampleResponse(200, groupExamples.getGroupResponse1("Group name"));
		addUser.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String groupUuid = ac.getParameter("groupUuid");
			String userUuid = ac.getParameter("userUuid");
			crudHandler.handleAddUserToGroup(ac, groupUuid, userUuid);
		});

		Endpoint removeUser = createEndpoint();
		removeUser.path("/:groupUuid/users/:userUuid").method(DELETE).produces(APPLICATION_JSON);
		removeUser.description("Remove the given user from the group.");
		removeUser.exampleResponse(200, groupExamples.getGroupResponse1("Group name"));
		removeUser.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String groupUuid = ac.getParameter("groupUuid");
			String userUuid = ac.getParameter("userUuid");
			crudHandler.handleRemoveUserFromGroup(ac, groupUuid, userUuid);
		});
	}

	private void addDeleteHandler() {
		Endpoint deleteGroup = createEndpoint();
		deleteGroup.path("/:uuid");
		deleteGroup.method(DELETE);
		deleteGroup.description("Delete the given group.");
		deleteGroup.exampleResponse(200, miscExamples.getMessageResponse());
		deleteGroup.produces(APPLICATION_JSON);
		deleteGroup.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	// TODO Determine what we should do about conflicting group names. Should we let neo4j handle those cases?
	// TODO update timestamps
	private void addUpdateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/:uuid");
		endpoint.method(PUT);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(groupExamples.getGroupUpdateRequest("New group name"));
		endpoint.exampleResponse(200, groupExamples.getGroupResponse1("New group name"));
		endpoint.description("Update the group with the given uuid.");
		endpoint.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleUpdate(ac, uuid);
		});

	}

	private void addReadHandler() {
		Endpoint readOne = createEndpoint();
		readOne.path("/:uuid");
		readOne.method(GET);
		readOne.description("Read the group with the given uuid.");
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(200, groupExamples.getGroupResponse1("Admin Group"));
		readOne.addQueryParameters(RolePermissionParameters.class);
		readOne.handler(rc -> {
			InternalActionContext ac = InternalActionContext.create(rc);
			String uuid = ac.getParameter("uuid");
			crudHandler.handleRead(ac, uuid);
		});

		/*
		 * List all groups when no parameter was specified
		 */
		Endpoint readAll = createEndpoint();
		readAll.path("/");
		readAll.method(GET);
		readAll.description("Read multiple groups and return a paged list response.");
		readAll.produces(APPLICATION_JSON);
		readAll.exampleResponse(200, groupExamples.getGroupListResponse());
		readAll.addQueryParameters(PagingParameters.class);
		readAll.addQueryParameters(RolePermissionParameters.class);
		readAll.handler(rc -> {
			crudHandler.handleReadList(InternalActionContext.create(rc));
		});
	}

	// TODO handle conflicting group name: group_conflicting_name
	private void addCreateHandler() {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new group");
		endpoint.exampleRequest(groupExamples.getGroupCreateRequest("New group"));
		endpoint.exampleResponse(201, groupExamples.getGroupResponse1("New group"));
		endpoint.handler(rc -> {
			crudHandler.handleCreate(InternalActionContext.create(rc));
		});

	}
}
