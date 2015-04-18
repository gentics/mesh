package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.Route;

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
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.user.request.UserCreateRequest;
import com.gentics.cailun.core.rest.user.request.UserUpdateRequest;
import com.gentics.cailun.core.rest.user.response.UserListResponse;
import com.gentics.cailun.core.rest.user.response.UserResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.path.PagingInfo;
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
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addReadHandler() {

		route("/:uuid").method(GET).handler(rc -> {
			vertx.executeBlocking(handler -> {
				User user;
				try (Transaction tx = graphDb.beginTx()) {
					user = getObject(rc, "uuid", PermissionType.READ);
					tx.success();
				}
				UserResponse restUser = userService.transformToRest(user);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(restUser));

			}, null);
		});

		/*
		 * List all users when no parameter was specified
		 */
		route("/").method(GET).handler(rc -> {
			UserListResponse listResponse = new UserListResponse();
			try (Transaction tx = graphDb.beginTx()) {
				PagingInfo pagingInfo = getPagingInfo(rc);
				// User requestUser = springConfiguration.authService().getUser(rc);
				User user = null;
				Page<User> userPage = userService.findAllVisible(user, pagingInfo);
				for (User currentUser : userPage) {
					listResponse.getData().add(userService.transformToRest(currentUser));
				}
				RestModelPagingHelper.setPaging(listResponse, userPage.getNumber(), userPage.getTotalPages(), pagingInfo.getPerPage(),
						userPage.getTotalElements());
				tx.success();
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(listResponse));
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			// TODO invalidate active sessions for this user
				try (Transaction tx = graphDb.beginTx()) {
					User user = getObject(rc, "uuid", PermissionType.DELETE);
					userService.delete(user);
					tx.success();
				}
				rc.response().setStatusCode(200);
				rc.response().end(toJson(new GenericMessageResponse(i18n.get(rc, "user_deleted", uuid))));
			});
	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON);
		route.handler(rc -> {
			User user = null;
			try (Transaction tx = graphDb.beginTx()) {
				user = getObject(rc, "uuid", PermissionType.UPDATE);
				UserUpdateRequest requestModel = fromJson(rc, UserUpdateRequest.class);

				if (requestModel.getUsername() != null && user.getUsername() != requestModel.getUsername()) {
					if (userService.findByUsername(requestModel.getUsername()) != null) {
						throw new HttpStatusCodeErrorException(409, i18n.get(rc, "user_conflicting_username"));
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
				tx.success();

			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(userService.transformToRest(user)));

		});
	}

	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON);
		route.handler(rc -> {
			User user = null;
			try (Transaction tx = graphDb.beginTx()) {

				UserCreateRequest requestModel = fromJson(rc, UserCreateRequest.class);
				if (requestModel == null) {
					// TODO exception would be nice, add i18n
					String message = "Could not parse request json.";
					rc.response().setStatusCode(400);
					rc.response().end(toJson(new GenericMessageResponse(message)));
					return;
				}
				if (StringUtils.isEmpty(requestModel.getPassword())) {
					throw new HttpStatusCodeErrorException(400, i18n.get(rc, "user_missing_password"));
				}
				if (StringUtils.isEmpty(requestModel.getUsername())) {
					throw new HttpStatusCodeErrorException(400, i18n.get(rc, "user_missing_username"));
				}
				String groupUuid = requestModel.getGroupUuid();
				if (StringUtils.isEmpty(groupUuid)) {
					throw new HttpStatusCodeErrorException(400, i18n.get(rc, "user_missing_parentgroup_field"));
				}
				// Load the parent group for the user
				Group parentGroup = getObjectByUUID(rc, groupUuid, PermissionType.CREATE);

				// Check for conflicting usernames
				if (userService.findByUsername(requestModel.getUsername()) != null) {
					String message = i18n.get(rc, "user_conflicting_username");
					throw new HttpStatusCodeErrorException(409, message);
				}

				user = new User(requestModel.getUsername());
				user.setFirstname(requestModel.getFirstname());
				user.setLastname(requestModel.getLastname());
				user.setEmailAddress(requestModel.getEmailAddress());
				user.setPasswordHash(springConfiguration.passwordEncoder().encode(requestModel.getPassword()));
				user = userService.save(user);

				// Add the user to the parent group and reload user
				parentGroup.addUser(user);
				parentGroup = groupService.save(parentGroup);

				roleService.addCRUDPermissionOnRole(rc, new CaiLunPermission(parentGroup, PermissionType.CREATE), user);

				tx.success();
			}
			user = userService.reload(user);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(userService.transformToRest(user)));

		});
	}
}
