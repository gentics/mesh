package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.ext.apex.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.user.request.UserCreateRequest;
import com.gentics.cailun.core.rest.user.request.UserUpdateRequest;
import com.gentics.cailun.core.rest.user.response.UserListResponse;
import com.gentics.cailun.core.rest.user.response.UserResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.paging.PagingInfo;
import com.gentics.cailun.util.RestModelPagingHelper;

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
			Future<Integer> depthFuture = getDepth(rc);
			loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<User> rh) -> {
			}, trh -> {
				User user = trh.result();
				UserResponse restUser = userService.transformToRest(user, depthFuture.result());
				rc.response().setStatusCode(200).end(toJson(restUser));
			});
		});

		/*
		 * List all users when no parameter was specified
		 */
		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			PagingInfo pagingInfo = getPagingInfo(rc);
			Future<Integer> depthFuture = getDepth(rc);

			vertx.executeBlocking((Future<UserListResponse> bch) -> {
				UserListResponse listResponse = new UserListResponse();

				Page<User> userPage = userService.findAllVisible(rc, pagingInfo);
				for (User currentUser : userPage) {
					listResponse.getData().add(userService.transformToRest(currentUser, depthFuture.result()));
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
			loadObject(rc, "uuid", PermissionType.DELETE, (AsyncResult<User> rh) -> {
				User user = rh.result();
				userService.delete(user);
			}, trh -> {
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "user_deleted", uuid))));
			});
		});
	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			loadObject(rc, "uuid", PermissionType.UPDATE, (AsyncResult<User> rh) -> {
				User user = rh.result();
				UserUpdateRequest requestModel = fromJson(rc, UserUpdateRequest.class);

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
				user = userService.save(user);

			}, trh -> {
				User user = trh.result();
				rc.response().setStatusCode(200).end(toJson(userService.transformToRest(user, 0)));
			});

		});
	}

	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {

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

			Future<User> userCreated = Future.future();
			// Load the parent group for the user
			loadObjectByUuid(rc, groupUuid, PermissionType.CREATE, (AsyncResult<Group> rh) -> {

				Group parentGroup = rh.result();

				if (userService.findByUsername(requestModel.getUsername()) != null) {
					String message = i18n.get(rc, "user_conflicting_username");
					rc.fail(new HttpStatusCodeErrorException(409, message));
					return;
				}

				User user = new User(requestModel.getUsername());
				user.setFirstname(requestModel.getFirstname());
				user.setLastname(requestModel.getLastname());
				user.setEmailAddress(requestModel.getEmailAddress());
				user.setPasswordHash(springConfiguration.passwordEncoder().encode(requestModel.getPassword()));
				user.getGroups().add(parentGroup);
				user = userService.save(user);
				roleService.addCRUDPermissionOnRole(rc, new CaiLunPermission(parentGroup, PermissionType.CREATE), user);
				userCreated.complete(user);
			}, trh -> {
				User user = userCreated.result();
				rc.response().setStatusCode(200).end(toJson(userService.transformToRest(user, 0)));
			});

		});
	}

}
