package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.ext.apex.Session;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.role.request.RoleCreateRequest;
import com.gentics.cailun.core.rest.role.request.RoleUpdateRequest;
import com.gentics.cailun.core.rest.role.response.RoleListResponse;
import com.gentics.cailun.core.rest.role.response.RoleResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.paging.PagingInfo;
import com.gentics.cailun.util.RestModelPagingHelper;

@Component
@Scope("singleton")
@SpringVerticle
public class RoleVerticle extends AbstractCoreApiVerticle {

	public RoleVerticle() {
		super("roles");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
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
			String uuid = rc.request().params().get("uuid");
			loadObject(rc, "uuid", PermissionType.DELETE, (AsyncResult<Role> rh) -> {
				Role role = rh.result();
				roleService.delete(role);
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "role_deleted", uuid))));
			});

		});
	}

	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).handler(rc -> {
			loadObject(rc, "uuid", PermissionType.UPDATE, (AsyncResult<Role> rh) -> {
				Role role = rh.result();
				RoleUpdateRequest requestModel = fromJson(rc, RoleUpdateRequest.class);

				if (!StringUtils.isEmpty(requestModel.getName()) && role.getName() != requestModel.getName()) {
					if (roleService.findByName(requestModel.getName()) != null) {
						rc.response().setStatusCode(409);
						throw new HttpStatusCodeErrorException(409, i18n.get(rc, "role_conflicting_name"));
					}
					role.setName(requestModel.getName());
				}
				role = roleService.save(role);
				rc.response().setStatusCode(200).end(toJson(roleService.transformToRest(role)));
			});
		});
	}

	private void addReadHandler() {
		route("/:uuid").method(GET).handler(rc -> {
			loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<Role> rh) -> {
				Role role = rh.result();
				RoleResponse restRole = roleService.transformToRest(role);
				rc.response().setStatusCode(200).end(toJson(restRole));
			});
		});

		/*
		 * List all roles when no parameter was specified
		 */
		route("/").method(GET).handler(rc -> {
			RoleListResponse listResponse = new RoleListResponse();

			PagingInfo pagingInfo = getPagingInfo(rc);
			User user = userService.findUser(rc);
			Page<Role> rolePage = roleService.findAllVisible(user, pagingInfo);
			for (Role role : rolePage) {
				listResponse.getData().add(roleService.transformToRest(role));
			}
			RestModelPagingHelper.setPaging(listResponse, rolePage, pagingInfo);
			rc.response().setStatusCode(200).end(toJson(listResponse));
		});
	}

	private void addCreateHandler() {
		route("/").method(POST).consumes(APPLICATION_JSON).handler(rc -> {
			RoleCreateRequest requestModel = fromJson(rc, RoleCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set")));
				return;
			}

			if (StringUtils.isEmpty(requestModel.getGroupUuid())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "role_missing_parentgroup_field")));
				return;
			}

			if (roleService.findByName(requestModel.getName()) != null) {
				rc.fail(new HttpStatusCodeErrorException(409, i18n.get(rc, "role_conflicting_name")));
				return;
			}

			loadObjectByUuid(rc, requestModel.getGroupUuid(), PermissionType.CREATE, (AsyncResult<Group> rh) -> {
				try (Transaction tx = graphDb.beginTx()) {
					Role role = new Role(requestModel.getName());
					Group parentGroup = rh.result();
					role.getGroups().add(parentGroup);
					role = roleService.save(role);

					roleService.addCRUDPermissionOnRole(rc, new CaiLunPermission(parentGroup, PermissionType.CREATE), role);
					rc.response().setStatusCode(200).end(toJson(roleService.transformToRest(role)));
					tx.success();
				}
			});

		});
	}
}
