package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.model.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.util.JsonUtils.fromJson;
import static com.gentics.mesh.util.JsonUtils.toJson;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.MeshAuthUser;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
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
			rcs.loadObject(rc, "uuid", DELETE_PERM, Role.class, (AsyncResult<Role> rh) -> {
				Role role = rh.result();
				role.delete();
			}, trh -> {
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "role_deleted", uuid))));
			});
		});
	}

	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).handler(rc -> {
			rcs.loadObject(rc, "uuid", UPDATE_PERM, Role.class, (AsyncResult<Role> rh) -> {
				Role role = rh.result();
				RoleUpdateRequest requestModel = fromJson(rc, RoleUpdateRequest.class);

				if (!StringUtils.isEmpty(requestModel.getName()) && role.getName() != requestModel.getName()) {
					if (roleService.findByName(requestModel.getName()) != null) {
						rc.fail(new HttpStatusCodeErrorException(409, i18n.get(rc, "role_conflicting_name")));
						return;
					}
					role.setName(requestModel.getName());
				}
			}, trh -> {
				Role role = trh.result();
				rc.response().setStatusCode(200).end(toJson(role.transformToRest()));
			});
		});
	}

	private void addReadHandler() {
		route("/:uuid").method(GET).handler(rc -> {
			rcs.loadObject(rc, "uuid", READ_PERM, Role.class, (AsyncResult<Role> rh) -> {
				Role role = rh.result();
				RoleResponse restRole = role.transformToRest();
				rc.response().setStatusCode(200).end(toJson(restRole));
			});
		});

		/*
		 * List all roles when no parameter was specified
		 */
		route("/").method(GET).handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);
			PagingInfo pagingInfo = getPagingInfo(rc);

			vertx.executeBlocking((Future<RoleListResponse> bch) -> {
				RoleListResponse listResponse = new RoleListResponse();
				Page<? extends Role> rolePage;
				try {
					rolePage = roleService.findAll(requestUser, pagingInfo);
					for (Role role : rolePage) {
						listResponse.getData().add(role.transformToRest());
					}
					RestModelPagingHelper.setPaging(listResponse, rolePage, pagingInfo);

					bch.complete(listResponse);
				} catch (Exception e) {
					rc.fail(e);
				}

			}, rh -> {
				if (rh.failed()) {
					rc.fail(rh.cause());
				}
				RoleListResponse listResponse = rh.result();
				rc.response().setStatusCode(200).end(toJson(listResponse));
			});

		});
	}

	private void addCreateHandler() {
		route("/").method(POST).consumes(APPLICATION_JSON).handler(rc -> {
			RoleCreateRequest requestModel = fromJson(rc, RoleCreateRequest.class);
			MeshAuthUser requestUser = getUser(rc);
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
			rcs.loadObjectByUuid(rc, requestModel.getGroupUuid(), CREATE_PERM, Group.class, (AsyncResult<Group> rh) -> {
				Group parentGroup = rh.result();
				Role role = parentGroup.createRole(requestModel.getName());
				role.addGroup(parentGroup);
				roleService.addCRUDPermissionOnRole(requestUser, parentGroup, CREATE_PERM, role);
				roleCreated.complete(role);
			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				Role role = roleCreated.result();
				rc.response().setStatusCode(200).end(toJson(role.transformToRest()));
			});

		});
	}
}
