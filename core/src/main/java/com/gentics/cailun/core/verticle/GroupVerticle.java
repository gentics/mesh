package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.core.Route;
import io.vertx.ext.apex.core.RoutingContext;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.RoleService;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.group.request.GroupCreateRequest;
import com.gentics.cailun.core.rest.group.request.GroupUpdateRequest;
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.error.HttpStatusCodeErrorException;

@Component
@Scope("singleton")
@SpringVerticle
public class GroupVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private GroupService groupService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private UserService userService;

	public GroupVerticle() {
		super("groups");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addCRUDHandlers();
	}

	private void addCRUDHandlers() {
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();

		addGroupChildGroupHandlers();
		addGroupUserHandlers();
		addGroupRoleHandlers();
	}

	private void addGroupRoleHandlers() {
		route("/:groupUuid/roles/:roleUuid").method(POST).handler(rc -> {
			Group group = getGroup(rc, "groupUuid", PermissionType.UPDATE);
			Role role = getObject(rc, "roleUuid", PermissionType.READ);

			if (group.addRole(role)) {
				group = groupService.save(group);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(groupService.transformToRest(group)));
			} else {
				// TODO 200?
			}
		});

		route("/:groupUuid/roles/:roleUuid").method(DELETE).handler(rc -> {
			Group group = getGroup(rc, "groupUuid", PermissionType.UPDATE);
			Role role = getObject(rc, "roleUuid", PermissionType.READ);

			if (group.removeRole(role)) {
				group = groupService.save(group);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(groupService.transformToRest(group)));
			} else {
				// TODO 200?
			}
		});
	}

	private void addGroupUserHandlers() {
		Route route = route("/:groupUuid/users/:userUuid").method(POST);
		route.handler(rc -> {
			Group group = getGroup(rc, "groupUuid", PermissionType.UPDATE);
			User user = getObject(rc, "userUuid", PermissionType.READ);

			if (group.addUser(user)) {
				group = groupService.save(group);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(groupService.transformToRest(group)));
			} else {
				// TODO 200?
			}

		});

		route = route("/:groupUuid/users/:userUuid").method(DELETE);
		route.handler(rc -> {
			Group group = getGroup(rc, "groupUuid", PermissionType.UPDATE);
			User user = getObject(rc, "userUuid", PermissionType.READ);

			if (group.removeUser(user)) {
				groupService.save(group);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(groupService.transformToRest(group)));
			} else {
				// TODO 200?
			}
		});
	}

	/**
	 * Extract the given uri parameter and load the group. Permissions and load verification will also be done by this method.
	 * 
	 * @param rc
	 * @param param
	 *            Name of the uri parameter which hold the uuid
	 * @param perm
	 *            Permission type which will be checked
	 * @return Loaded group. Can't be null.
	 */
	private Group getGroup(RoutingContext rc, String param, PermissionType perm) {
		// TODO check whether getObject would also work
		String groupUuid = rc.request().params().get(param);
		if (StringUtils.isEmpty(groupUuid)) {
			throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_request_parameter_missing", param));
		}
		return getGroupByUuid(rc, groupUuid, perm);
	}

	/**
	 * Load the group with the given uuid and check selected permissions.
	 * 
	 * @param rc
	 * @param uuid
	 * @param perm
	 * @return
	 */
	private Group getGroupByUuid(RoutingContext rc, String uuid, PermissionType perm) {
		Group group = groupService.findByUUID(uuid);
		if (group == null) {
			throw new EntityNotFoundException(i18n.get(rc, "group_not_found_for_uuid", uuid));
		}
		failOnMissingPermission(rc, group, perm);
		return group;
	}

	private void addGroupChildGroupHandlers() {
		Route postRoute = route("/:groupUuid1/groups/:groupUuid2").method(POST);
		postRoute.handler(rc -> {
			Group group = getGroup(rc, "groupUuid1", PermissionType.UPDATE);
			Group group2 = getGroup(rc, "groupUuid2", PermissionType.READ);

			if (group.addGroup(group2)) {
				groupService.save(group);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(groupService.transformToRest(group)));
			} else {
				// TODO 200?
			}

			rc.response().setStatusCode(200);

		});

		Route deleteRoute = route("/:groupUuid1/groups/:groupUuid2").method(DELETE);
		deleteRoute.handler(rc -> {
			Group group = getGroup(rc, "groupUuid1", PermissionType.UPDATE);
			Group group2 = getGroup(rc, "groupUuid2", PermissionType.READ);

			if (group.removeGroup(group2)) {
				groupService.save(group);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(groupService.transformToRest(group)));
			} else {
				// TODO 200?
			}

			rc.response().setStatusCode(200);

		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			Group group = getGroup(rc, "uuid", PermissionType.DELETE);
			groupService.delete(group);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(new GenericMessageResponse("Deleted")));
		});
	}

	private void addUpdateHandler() {
		route("/:uuid").method(PUT).handler(rc -> {
			Group group = getGroup(rc, "uuid", PermissionType.UPDATE);
			GroupUpdateRequest requestModel = fromJson(rc, GroupUpdateRequest.class);

			if (StringUtils.isEmpty(group.getName())) {
				// TODO i18n
				throw new HttpStatusCodeErrorException(400, "Name can't be empty or null");
			}
			if (!group.getName().equals(requestModel.getName())) {
				group.setName(requestModel.getName());
			}

			// TODO update timestamps

			group = groupService.save(group);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(groupService.transformToRest(group)));
		});

	}

	private void addReadHandler() {
		route("/:uuid").method(GET).handler(rc -> {
			Group group = getGroup(rc, "uuid", PermissionType.READ);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(groupService.transformToRest(group)));
		});
	}

	private void addCreateHandler() {
		route("/").method(POST).handler(rc -> {

			GroupCreateRequest requestModel = fromJson(rc, GroupCreateRequest.class);
			String parentGroupUuid = requestModel.getGroupUuid();
			if (StringUtils.isEmpty(parentGroupUuid)) {
				// TODO i18n
				throw new HttpStatusCodeErrorException(400, "The group uuid field has not been set. Parent group must be specified.");
			}
			Group parentGroup = getGroupByUuid(rc, parentGroupUuid, PermissionType.UPDATE);

			Group group = new Group(requestModel.getName());
			parentGroup.addGroup(group);

			group = groupService.save(group);
			group = groupService.reload(group);

			parentGroup = groupService.save(parentGroup);

			rc.response().setStatusCode(200);
			rc.response().end(toJson(groupService.transformToRest(group)));

		});

	}
}
