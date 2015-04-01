package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
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
import com.gentics.cailun.path.PagingInfo;
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
			try (Transaction tx = graphDb.beginTx()) {
				Role role = getObject(rc, "uuid", PermissionType.DELETE);
				roleService.delete(role);
				tx.success();
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(new GenericMessageResponse(i18n.get(rc, "role_deleted", uuid))));

		});
	}

	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).handler(rc -> {
			Role role;
			try (Transaction tx = graphDb.beginTx()) {

				role = getObject(rc, "uuid", PermissionType.UPDATE);
				RoleUpdateRequest requestModel = fromJson(rc, RoleUpdateRequest.class);

				if (!StringUtils.isEmpty(requestModel.getName()) && role.getName() != requestModel.getName()) {
					if (roleService.findByName(requestModel.getName()) != null) {
						rc.response().setStatusCode(409);
						throw new HttpStatusCodeErrorException(409, i18n.get(rc, "role_conflicting_name"));
					}
					role.setName(requestModel.getName());
				}
				role = roleService.save(role);
				tx.success();
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(roleService.transformToRest(role)));
		});
	}

	private void addReadHandler() {
		route("/:uuid").method(GET).handler(rc -> {
			Role role;
			try (Transaction tx = graphDb.beginTx()) {
				role = getObject(rc, "uuid", PermissionType.READ);
				tx.success();
			}
			RoleResponse restRole = roleService.transformToRest(role);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(restRole));
		});

		/*
		 * List all roles when no parameter was specified
		 */
		route("/").method(GET).handler(
				rc -> {
					Session session = rc.session();
					RoleListResponse listResponse = new RoleListResponse();


					try (Transaction tx = graphDb.beginTx()) {
						PagingInfo pagingInfo = getPagingInfo(rc);
						User requestUser = springConfiguration.authService().getUser(rc);
						Page<Role> rolePage = roleService.findAllVisible(requestUser, pagingInfo);
						for (Role role : rolePage) {
							listResponse.getData().add(roleService.transformToRest(role));
						}
						RestModelPagingHelper.setPaging(listResponse, rolePage.getNumber(), rolePage.getTotalPages(), pagingInfo.getPerPage(),
								rolePage.getTotalElements());
						tx.success();
					}
					rc.response().setStatusCode(200);
					rc.response().end(toJson(listResponse));
				});
	}

	private void addCreateHandler() {
		route("/").method(POST).consumes(APPLICATION_JSON).handler(rc -> {
			RoleCreateRequest requestModel = fromJson(rc, RoleCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set"));
			}

			if (StringUtils.isEmpty(requestModel.getGroupUuid())) {
				throw new HttpStatusCodeErrorException(400, i18n.get(rc, "role_missing_parentgroup_field"));
			}
			Role role;
			try (Transaction tx = graphDb.beginTx()) {

				if (roleService.findByName(requestModel.getName()) != null) {
					throw new HttpStatusCodeErrorException(409, i18n.get(rc, "role_conflicting_name"));
				}

				Group parentGroup = getObjectByUUID(rc, requestModel.getGroupUuid(), PermissionType.CREATE);

				role = new Role(requestModel.getName());
				role = roleService.save(role);
				parentGroup.addRole(role);
				groupService.save(parentGroup);

				roleService.addCRUDPermissionOnRole(rc, new CaiLunPermission(parentGroup, PermissionType.CREATE), role);

				tx.success();
			}
			role = roleService.reload(role);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(roleService.transformToRest(role)));
		});
	}
}
