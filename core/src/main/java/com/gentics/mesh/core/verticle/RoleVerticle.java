package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.util.JsonUtils.fromJson;
import static com.gentics.mesh.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.model.auth.MeshPermission;
import com.gentics.mesh.core.data.model.auth.Group;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.auth.Role;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.rest.common.response.GenericMessageResponse;
import com.gentics.mesh.core.rest.role.request.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.request.RoleUpdateRequest;
import com.gentics.mesh.core.rest.role.response.RoleListResponse;
import com.gentics.mesh.core.rest.role.response.RoleResponse;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.RestModelPagingHelper;

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
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			rcs.loadObject(rc, "uuid", PermissionType.DELETE, (AsyncResult<Role> rh) -> {
				Role role = rh.result();
				roleService.delete(role);
			}, trh -> {
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "role_deleted", uuid))));
			});
		});
	}

	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).handler(rc -> {
			rcs.loadObject(rc, "uuid", PermissionType.UPDATE, (AsyncResult<Role> rh) -> {
				Role role = rh.result();
				RoleUpdateRequest requestModel = fromJson(rc, RoleUpdateRequest.class);

				if (!StringUtils.isEmpty(requestModel.getName()) && role.getName() != requestModel.getName()) {
					if (roleService.findByName(requestModel.getName()) != null) {
						rc.fail(new HttpStatusCodeErrorException(409, i18n.get(rc, "role_conflicting_name")));
						return;
					}
					role.setName(requestModel.getName());
				}
				role = roleService.save(role);
			}, trh -> {
				Role role = trh.result();
				rc.response().setStatusCode(200).end(toJson(roleService.transformToRest(role)));
			});
		});
	}

	private void addReadHandler() {
		route("/:uuid").method(GET).handler(rc -> {
			rcs.loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<Role> rh) -> {
				Role role = rh.result();
				RoleResponse restRole = roleService.transformToRest(role);
				rc.response().setStatusCode(200).end(toJson(restRole));
			});
		});

		/*
		 * List all roles when no parameter was specified
		 */
		route("/").method(GET).handler(rc -> {

			PagingInfo pagingInfo = rcs.getPagingInfo(rc);

			vertx.executeBlocking((Future<RoleListResponse> bch) -> {
				RoleListResponse listResponse = new RoleListResponse();
				User user = userService.findUser(rc);
				Page<Role> rolePage = roleService.findAllVisible(user, pagingInfo);
				for (Role role : rolePage) {
					listResponse.getData().add(roleService.transformToRest(role));
				}
				RestModelPagingHelper.setPaging(listResponse, rolePage, pagingInfo);

				bch.complete(listResponse);
			}, rh -> {
				RoleListResponse listResponse = rh.result();
				rc.response().setStatusCode(200).end(toJson(listResponse));
			});

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
			Future<Role> roleCreated = Future.future();
			rcs.loadObjectByUuid(rc, requestModel.getGroupUuid(), PermissionType.CREATE, (AsyncResult<Group> rh) -> {
				Role role = new Role(requestModel.getName());
				Group parentGroup = rh.result();
				role.getGroups().add(parentGroup);
				role = roleService.save(role);
				roleService.addCRUDPermissionOnRole(rc, new MeshPermission(parentGroup, PermissionType.CREATE), role);
				roleCreated.complete(role);
			}, trh -> {
				Role role = roleCreated.result();
				rc.response().setStatusCode(200).end(toJson(roleService.transformToRest(role)));
			});

		});
	}
}
