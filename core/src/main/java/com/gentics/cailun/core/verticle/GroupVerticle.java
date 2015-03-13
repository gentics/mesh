package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.RoleService;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.group.response.GroupResponse;
import com.gentics.cailun.error.EntityNotFoundException;

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
		route("/:groupUuid/roles/:roleUuid").method(PUT).handler(rc -> {
			String groupUuid = rc.request().params().get("groupUuid");
			String roleUuid = rc.request().params().get("roleUuid");
			Group group = groupService.findByUUID(groupUuid);
			Role role = roleService.findByUUID(roleUuid);
		});
		route("/:groupUuid/roles/:roleUuid").method(DELETE).handler(rc -> {
			String groupUuid = rc.request().params().get("groupUuid");
			String roleUuid = rc.request().params().get("roleUuid");
			Group group = groupService.findByUUID(groupUuid);
			Role role = roleService.findByUUID(roleUuid);
		});
	}

	private void addGroupUserHandlers() {
		route("/:groupUuid/users/:userUuid").method(PUT).handler(rc -> {
			String groupUuid = rc.request().params().get("groupUuid");
			String userUuid = rc.request().params().get("userUuid");
			Group group = groupService.findByUUID(groupUuid);
			User user = userService.findByUUID(userUuid);
		});
		route("/:groupUuid/users/:userUuid").method(DELETE).handler(rc -> {
			String groupUuid = rc.request().params().get("groupUuid");
			String userUuid = rc.request().params().get("userUuid");
			Group group = groupService.findByUUID(groupUuid);
			User user = userService.findByUUID(userUuid);
		});
	}

	private void addGroupChildGroupHandlers() {
		route("/:groupUuid1/groups/:groupUuid2").method(PUT).handler(rc -> {
			String groupUuid1 = rc.request().params().get("groupUuid1");
			String groupUuid2 = rc.request().params().get("groupUuid2");
			Group group1 = groupService.findByUUID(groupUuid1);
			Group group2 = groupService.findByUUID(groupUuid2);

		});
		route("/:groupUuid1/groups/:groupUuid2").method(DELETE).handler(rc -> {
			String groupUuid1 = rc.request().params().get("groupUuid1");
			String groupUuid2 = rc.request().params().get("groupUuid2");
			Group group1 = groupService.findByUUID(groupUuid1);
			Group group2 = groupService.findByUUID(groupUuid2);
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (uuid == null) {
				// TODO handle this case
			}
			Group group = groupService.findByUUID(uuid);

			if (group != null) {
				groupService.delete(group);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(new GenericMessageResponse("Deleted")));
			} else {
				String message = i18n.get(rc, "group_not_found_for_uuid", uuid);
				throw new EntityNotFoundException(message);
			}
		});
	}

	private void addUpdateHandler() {
		route("/").method(PUT).handler(rc -> {
			// TODO read model
				String uuid = null;
				Group group = groupService.findByUUID(uuid);

				if (group != null) {
					// groupService.save(node);
					rc.response().setStatusCode(200);
					rc.response().end(toJson(new GenericMessageResponse("OK")));
				} else {
					String message = i18n.get(rc, "group_not_found_for_uuid", uuid);
					throw new EntityNotFoundException(message);
				}
			});

	}

	private void addReadHandler() {
		route("/:uuid").method(GET).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (uuid == null) {
				// TODO handle this case
			}
			Group group = groupService.findByUUID(uuid);

			if (group != null) {
				GroupResponse restGroup = groupService.transformToRest(group);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(restGroup));
			} else {
				String message = i18n.get(rc, "group_not_found_for_uuid", uuid);
				throw new EntityNotFoundException(message);
			}

		});

	}

	private void addCreateHandler() {
		route("/").method(POST).handler(rc -> {

		});

	}

}
