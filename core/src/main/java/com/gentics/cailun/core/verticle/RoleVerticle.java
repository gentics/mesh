package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.Session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.RoleService;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.role.request.RoleCreateRequest;
import com.gentics.cailun.core.rest.role.request.RoleUpdateRequest;
import com.gentics.cailun.core.rest.role.response.RoleResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;

@Component
@Scope("singleton")
@SpringVerticle
public class RoleVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private RoleService roleService;

	@Autowired
	private GroupService groupService;

	public RoleVerticle() {
		super("roles");
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

		// addRoleGroupHandlers();
	}

	// private void addRoleGroupHandlers() {
	// // TODO document needed permissions
	// route("/:roleUuid/groups/:groupUuid").method(PUT).handler(rc -> {
	// Role role = getObject(rc, "roleUuid", PermissionType.READ);
	// Group group = getObject(rc, "groupUuid", PermissionType.UPDATE);
	//
	// if (group.addRole(role)) {
	// group = groupService.save(group);
	// // TODO reload role?
	// rc.response().setStatusCode(200);
	// rc.response().end(toJson(roleService.transformToRest(role)));
	// } else {
	// // TODO 200?
	// }
	// });
	// route("/:roleUuid/groups/:groupUuid").method(DELETE).handler(rc -> {
	// Role role = getObject(rc, "roleUuid", PermissionType.READ);
	// Group group = getObject(rc, "groupUuid", PermissionType.UPDATE);
	//
	// if (group.removeRole(role)) {
	// group = groupService.save(group);
	// // TODO reload role?
	// rc.response().setStatusCode(200);
	// rc.response().end(toJson(roleService.transformToRest(role)));
	// } else {
	// // TODO 200?
	// }
	// });
	//
	// }

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			Role role = getObject(rc, "uuid", PermissionType.DELETE);
			roleService.delete(role);
			rc.response().setStatusCode(200);
			String uuid = rc.request().params().get("uuid");
			rc.response().end(toJson(new GenericMessageResponse(i18n.get(rc, "role_deleted", uuid))));
			return;
		});
	}

	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).handler(rc -> {
			Role role = getObject(rc, "uuid", PermissionType.UPDATE);
			RoleUpdateRequest requestModel = fromJson(rc, RoleUpdateRequest.class);

			if (!StringUtils.isEmpty(requestModel.getName()) && role.getName() != requestModel.getName()) {
				if (roleService.findByName(requestModel.getName()) != null) {
					rc.response().setStatusCode(409);
					// TODO i18n
				String message = "A role with the name {" + requestModel.getName() + "} already exists. Please choose a different name.";
				throw new HttpStatusCodeErrorException(409, message);
			}
			role.setName(requestModel.getName());
		}
		try {
			role = roleService.save(role);
		} catch (ConstraintViolationException e) {
			// TODO correct msg?
			// TODO i18n
			throw new HttpStatusCodeErrorException(409, "Role can't be saved. Unknown error.", e);
		}
		rc.response().setStatusCode(200);
		rc.response().end(toJson(roleService.transformToRest(role)));
	}	);
	}

	private void addReadHandler() {
		route("/:uuid").method(GET).handler(rc -> {
			Role role = getObject(rc, "uuid", PermissionType.READ);
			RoleResponse restRole = roleService.transformToRest(role);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(restRole));
		});

		/*
		 * List all roles when no parameter was specified
		 */
		route("/").method(GET).handler(rc -> {
			Session session = rc.session();
			Map<String, RoleResponse> resultMap = new HashMap<>();
			List<Role> roles = roleService.findAll();
			for (Role role : roles) {
				boolean hasPerm = getAuthService().hasPermission(session.getLoginID(), new CaiLunPermission(role, PermissionType.READ));
				if (hasPerm) {
					resultMap.put(role.getName(), roleService.transformToRest(role));
				}
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(resultMap));
			return;
		});
	}

	private void addCreateHandler() {
		route("/").method(POST).consumes(APPLICATION_JSON).handler(rc -> {
			RoleCreateRequest requestModel = fromJson(rc, RoleCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.response().setStatusCode(400);
				// TODO i18n
				String message = "The name for the role was not specified.";
				throw new HttpStatusCodeErrorException(400, message);
			}

			if (StringUtils.isEmpty(requestModel.getGroupUuid())) {
				rc.response().setStatusCode(400);
				// TODO i18n
				String message = "The group id for the role was not specified.";
				throw new HttpStatusCodeErrorException(400, message);
			}

			if (roleService.findByName(requestModel.getName()) != null) {
				// TODO i18n
				String message = "Conflicting name";
				throw new HttpStatusCodeErrorException(409, message);
			}

			Group group = getObjectByUUID(rc, requestModel.getGroupUuid(), PermissionType.UPDATE);

			Role role = new Role(requestModel.getName());
			role = roleService.save(role);
			role = roleService.reload(role);
			group.addRole(role);
			groupService.save(group);

			RoleResponse restRole = roleService.transformToRest(role);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(restRole));

		});
	}
}
