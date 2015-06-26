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
import io.vertx.ext.web.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.Group;
import com.gentics.mesh.core.data.model.MeshAuthUser;
import com.gentics.mesh.core.data.model.MeshUser;
import com.gentics.mesh.core.rest.common.response.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.request.UserCreateRequest;
import com.gentics.mesh.core.rest.user.request.UserUpdateRequest;
import com.gentics.mesh.core.rest.user.response.UserListResponse;
import com.gentics.mesh.core.rest.user.response.UserResponse;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.BlueprintTransaction;
import com.gentics.mesh.util.RestModelPagingHelper;

@Component
@Scope("singleton")
@SpringVerticle
public class UserVerticle extends AbstractCoreApiVerticle {

	public UserVerticle() {
		super("users");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addReadHandler() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			rcs.loadObject(rc, "uuid", READ_PERM, MeshUser.class, (AsyncResult<MeshUser> rh) -> {
			}, trh -> {
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					MeshUser user = trh.result();
					UserResponse restUser = user.transformToRest();
					tx.success();
					rc.response().setStatusCode(200).end(toJson(restUser));
				}
			});
		});

		/*
		 * List all users when no parameter was specified
		 */
		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);
			PagingInfo pagingInfo = getPagingInfo(rc);
			vertx.executeBlocking((Future<UserListResponse> bch) -> {
				UserListResponse listResponse = new UserListResponse();

				Page<MeshUser> userPage = userService.findAllVisible(requestUser, pagingInfo);
				for (MeshUser currentUser : userPage) {
					listResponse.getData().add(currentUser.transformToRest());
				}
				RestModelPagingHelper.setPaging(listResponse, userPage, pagingInfo);
				bch.complete(listResponse);
			}, arh -> {
				UserListResponse list = arh.result();
				rc.response().setStatusCode(200).end(toJson(list));
			});
		});
	}

	// TODO invalidate active sessions for this user
	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			rcs.loadObject(rc, "uuid", DELETE_PERM, MeshUser.class, (AsyncResult<MeshUser> rh) -> {
				MeshUser user = rh.result();
				user.delete();
			}, trh -> {
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "user_deleted", uuid))));
			});
		});
	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			rcs.loadObject(rc, "uuid", UPDATE_PERM, MeshUser.class, (AsyncResult<MeshUser> rh) -> {
				MeshUser user = rh.result();
				UserUpdateRequest requestModel = fromJson(rc, UserUpdateRequest.class);

				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					if (requestModel.getUsername() != null && user.getUsername() != requestModel.getUsername()) {
						if (userService.findByUsername(requestModel.getUsername()) != null) {
							rc.fail(new HttpStatusCodeErrorException(409, i18n.get(rc, "user_conflicting_username")));
							return;
						}
						user.setUsername(requestModel.getUsername());
					}

					if (!StringUtils.isEmpty(requestModel.getFirstname()) && user.getFirstname() != requestModel.getFirstname()) {
						user.setFirstname(requestModel.getFirstname());
					}

					if (!StringUtils.isEmpty(requestModel.getLastname()) && user.getLastname() != requestModel.getLastname()) {
						user.setLastname(requestModel.getLastname());
					}

					if (!StringUtils.isEmpty(requestModel.getEmailAddress()) && user.getEmailAddress() != requestModel.getEmailAddress()) {
						user.setEmailAddress(requestModel.getEmailAddress());
					}

					if (!StringUtils.isEmpty(requestModel.getPassword())) {
						user.setPasswordHash(springConfiguration.passwordEncoder().encode(requestModel.getPassword()));
					}
					tx.success();
					fg.commit();
				}
				fg.commit();
			}, trh -> {
				MeshUser user = trh.result();
				rc.response().setStatusCode(200).end(toJson(user.transformToRest()));
			});

		});
	}

	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {

			MeshAuthUser requestUser = getUser(rc);

			UserCreateRequest requestModel = fromJson(rc, UserCreateRequest.class);
			if (requestModel == null) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_parse_request_json_error")));
				return;
			}
			if (StringUtils.isEmpty(requestModel.getPassword())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "user_missing_password")));
				return;
			}
			if (StringUtils.isEmpty(requestModel.getUsername())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "user_missing_username")));
				return;
			}
			String groupUuid = requestModel.getGroupUuid();
			if (StringUtils.isEmpty(groupUuid)) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "user_missing_parentgroup_field")));
				return;
			}

			Future<MeshUser> userCreated = Future.future();
			// Load the parent group for the user
			rcs.loadObjectByUuid(rc, groupUuid, CREATE_PERM, Group.class, (AsyncResult<Group> rh) -> {

				Group parentGroup = rh.result();

				if (userService.findByUsername(requestModel.getUsername()) != null) {
					String message = i18n.get(rc, "user_conflicting_username");
					rc.fail(new HttpStatusCodeErrorException(409, message));
					return;
				}

				MeshUser user = parentGroup.createUser(requestModel.getUsername());
				user.setFirstname(requestModel.getFirstname());
				user.setLastname(requestModel.getLastname());
				user.setEmailAddress(requestModel.getEmailAddress());
				user.setPasswordHash(springConfiguration.passwordEncoder().encode(requestModel.getPassword()));
				user.addGroup(parentGroup);
				roleService.addCRUDPermissionOnRole(requestUser, parentGroup, CREATE_PERM, user);
				userCreated.complete(user);
			}, trh -> {
				MeshUser user = userCreated.result();
				rc.response().setStatusCode(200).end(toJson(user.transformToRest()));
			});

		});
	}

}
