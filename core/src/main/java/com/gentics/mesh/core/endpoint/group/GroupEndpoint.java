package com.gentics.mesh.core.endpoint.group;

import static com.gentics.mesh.example.ExampleUuids.GROUP_CLIENT_UUID;
import static com.gentics.mesh.example.ExampleUuids.GROUP_EDITORS_UUID;
import static com.gentics.mesh.example.ExampleUuids.ROLE_CLIENT_UUID;
import static com.gentics.mesh.example.ExampleUuids.USER_EDITOR_UUID;
import static com.gentics.mesh.example.ExampleUuids.USER_WEBCLIENT_UUID;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import javax.inject.Inject;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

public class GroupEndpoint extends AbstractInternalEndpoint {

	private GroupCrudHandler crudHandler;

	public GroupEndpoint() {
		super("groups", null);
	}

	@Inject
	public GroupEndpoint(MeshAuthChain chain, GroupCrudHandler crudHandler) {
		super("groups", chain);
		this.crudHandler = crudHandler;
	}

	@Override
	public String getDescription() {
		return "Provides endpoints which allow the manipulation of groups.";
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		addGroupUserHandlers();
		addGroupRoleHandlers();

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addGroupRoleHandlers() {
		InternalEndpointRoute readRoles = createRoute();
		readRoles.path("/:groupUuid/roles");
		readRoles.addUriParameter("groupUuid", "Uuid of the group.", GROUP_CLIENT_UUID);
		readRoles.description("Load multiple roles that are assigned to the group. Return a paged list response.");
		readRoles.method(GET);
		readRoles.produces(APPLICATION_JSON);
		readRoles.exampleResponse(OK, roleExamples.getRoleListResponse(), "List of roles which were assigned to the group.");
		readRoles.addQueryParameters(PagingParametersImpl.class);
		readRoles.addQueryParameters(RolePermissionParametersImpl.class);
		readRoles.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String groupUuid = ac.getParameter("groupUuid");
			crudHandler.handleGroupRolesList(ac, groupUuid);
		});

		InternalEndpointRoute addRole = createRoute();
		addRole.path("/:groupUuid/roles/:roleUuid");
		addRole.addUriParameter("groupUuid", "Uuid of the group.", GROUP_CLIENT_UUID);
		addRole.addUriParameter("roleUuid", "Uuid of the role.", ROLE_CLIENT_UUID);
		addRole.method(POST);
		addRole.description("Add the specified role to the group.");
		addRole.produces(APPLICATION_JSON);
		addRole.exampleResponse(OK, groupExamples.getGroupResponse1("Group name"), "Loaded role.");
		addRole.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String groupUuid = ac.getParameter("groupUuid");
			String roleUuid = ac.getParameter("roleUuid");
			crudHandler.handleAddRoleToGroup(ac, groupUuid, roleUuid);
		});

		InternalEndpointRoute removeRole = createRoute();
		removeRole.path("/:groupUuid/roles/:roleUuid");
		removeRole.addUriParameter("groupUuid", "Uuid of the group.", GROUP_CLIENT_UUID);
		removeRole.addUriParameter("roleUuid", "Uuid of the role.", ROLE_CLIENT_UUID);
		removeRole.method(DELETE);
		removeRole.description("Remove the given role from the group.");
		removeRole.exampleResponse(NO_CONTENT, "Role was removed from the group.");
		removeRole.produces(APPLICATION_JSON).handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String groupUuid = ac.getParameter("groupUuid");
			String roleUuid = ac.getParameter("roleUuid");
			crudHandler.handleRemoveRoleFromGroup(ac, groupUuid, roleUuid);
		});
	}

	private void addGroupUserHandlers() {
		InternalEndpointRoute readUsers = createRoute();
		readUsers.path("/:groupUuid/users");
		readUsers.addUriParameter("groupUuid", "Uuid of the group.", GROUP_CLIENT_UUID);
		readUsers.method(GET);
		readUsers.produces(APPLICATION_JSON);
		readUsers.exampleResponse(OK, userExamples.getUserListResponse(), "List of users which belong to the group.");
		readUsers.description("Load a list of users which have been assigned to the group.");
		readUsers.addQueryParameters(PagingParametersImpl.class);
		readUsers.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String groupUuid = ac.getParameter("groupUuid");
			crudHandler.handleGroupUserList(ac, groupUuid);
		});

		InternalEndpointRoute addUser = createRoute();
		addUser.path("/:groupUuid/users/:userUuid");
		addUser.addUriParameter("groupUuid", "Uuid of the group.", GROUP_CLIENT_UUID);
		addUser.addUriParameter("userUuid", "Uuid of the user which should be added to the group.", USER_EDITOR_UUID);
		addUser.method(POST);
		addUser.description("Add the given user to the group");
		addUser.produces(APPLICATION_JSON);
		addUser.exampleResponse(OK, groupExamples.getGroupResponse1("Group name"), "Updated group.");
		addUser.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String groupUuid = ac.getParameter("groupUuid");
			String userUuid = ac.getParameter("userUuid");
			crudHandler.handleAddUserToGroup(ac, groupUuid, userUuid);
		});

		InternalEndpointRoute removeUser = createRoute();
		removeUser.path("/:groupUuid/users/:userUuid").method(DELETE).produces(APPLICATION_JSON);
		removeUser.addUriParameter("groupUuid", "Uuid of the group.", GROUP_CLIENT_UUID);
		removeUser.addUriParameter("userUuid", "Uuid of the user which should be removed from the group.", USER_WEBCLIENT_UUID);
		removeUser.description("Remove the given user from the group.");
		removeUser.exampleResponse(NO_CONTENT, "User was removed from the group.");
		removeUser.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String groupUuid = ac.getParameter("groupUuid");
			String userUuid = ac.getParameter("userUuid");
			crudHandler.handleRemoveUserFromGroup(ac, groupUuid, userUuid);
		});
	}

	private void addDeleteHandler() {
		InternalEndpointRoute deleteGroup = createRoute();
		deleteGroup.path("/:groupUuid");
		deleteGroup.addUriParameter("groupUuid", "Uuid of the group which should be deleted.", GROUP_EDITORS_UUID);
		deleteGroup.description("Delete the given group.");
		deleteGroup.method(DELETE);
		deleteGroup.exampleResponse(NO_CONTENT, "Group was deleted.");
		deleteGroup.produces(APPLICATION_JSON);
		deleteGroup.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("groupUuid");
			crudHandler.handleDelete(ac, uuid);
		});
	}

	// TODO Determine what we should do about conflicting group names. Should we let neo4j handle those cases?
	// TODO update timestamps
	private void addUpdateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/:groupUuid");
		endpoint.addUriParameter("groupUuid", "Uuid of the group which should be updated.", GROUP_CLIENT_UUID);
		endpoint.description("Update the group with the given uuid. The group is created if no group with the specified uuid could be found.");
		endpoint.method(POST);
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.exampleRequest(groupExamples.getGroupUpdateRequest("New group name"));
		endpoint.exampleResponse(OK, groupExamples.getGroupResponse1("New group name"), "Updated group.");
		endpoint.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("groupUuid");
			crudHandler.handleUpdate(ac, uuid);
		});

	}

	private void addReadHandler() {
		InternalEndpointRoute readOne = createRoute();
		readOne.path("/:groupUuid");
		readOne.addUriParameter("groupUuid", "Uuid of the group.", GROUP_CLIENT_UUID);
		readOne.description("Read the group with the given uuid.");
		readOne.method(GET);
		readOne.produces(APPLICATION_JSON);
		readOne.exampleResponse(OK, groupExamples.getGroupResponse1("Admin Group"), "Loaded group.");
		readOne.addQueryParameters(RolePermissionParametersImpl.class);
		readOne.addQueryParameters(GenericParametersImpl.class);
		readOne.handler(rc -> {
			InternalActionContext ac = wrap(rc);
			String uuid = ac.getParameter("groupUuid");
			crudHandler.handleRead(ac, uuid);
		});

		/*
		 * List all groups when no parameter was specified
		 */
		InternalEndpointRoute readAll = createRoute();
		readAll.path("/");
		readAll.method(GET);
		readAll.description("Read multiple groups and return a paged list response.");
		readAll.produces(APPLICATION_JSON);
		readAll.exampleResponse(OK, groupExamples.getGroupListResponse(), "List response which contains the found  groups.");
		readAll.addQueryParameters(PagingParametersImpl.class);
		readAll.addQueryParameters(RolePermissionParametersImpl.class);
		readAll.addQueryParameters(GenericParametersImpl.class);
		readAll.handler(rc -> {
			crudHandler.handleReadList(wrap(rc));
		});
	}

	// TODO handle conflicting group name: group_conflicting_name
	private void addCreateHandler() {
		InternalEndpointRoute endpoint = createRoute();
		endpoint.path("/");
		endpoint.method(POST);
		endpoint.description("Create a new group.");
		endpoint.exampleRequest(groupExamples.getGroupCreateRequest("New group"));
		endpoint.exampleResponse(CREATED, groupExamples.getGroupResponse1("New group"), "Created group.");
		endpoint.handler(rc -> {
			crudHandler.handleCreate(wrap(rc));
		});

	}
}
