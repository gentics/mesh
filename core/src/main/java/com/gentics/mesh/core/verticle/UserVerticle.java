package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.Future;
import io.vertx.ext.web.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.util.BlueprintTransaction;

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
			loadObject(rc, "uuid", READ_PERM, boot.userRoot(), rh -> {
				loadTransformAndResponde(rc, "uuid", READ_PERM, boot.userRoot());
			});
		});

		/*
		 * List all users when no parameter was specified
		 */
		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			loadTransformAndResponde(rc, boot.userRoot(), new UserListResponse());
		});
	}

	// TODO invalidate active sessions for this user
	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			delete(rc, "uuid", "user_deleted", boot.userRoot());
		});
	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			loadObject(rc, "uuid", UPDATE_PERM, boot.userRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					User user = rh.result();
					UserUpdateRequest requestModel = fromJson(rc, UserUpdateRequest.class);

					try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
						if (requestModel.getUsername() != null && user.getUsername() != requestModel.getUsername()) {
							if (boot.userRoot().findByUsername(requestModel.getUsername()) != null) {
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
					}
					transformAndResponde(rc, user);
				}
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
			loadObjectByUuid(rc, groupUuid, CREATE_PERM, boot.groupRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					Group parentGroup = rh.result();
					if (boot.userRoot().findByUsername(requestModel.getUsername()) != null) {
						String message = i18n.get(rc, "user_conflicting_username");
						rc.fail(new HttpStatusCodeErrorException(409, message));
					} else {
						MeshAuthUser requestUser = getUser(rc);
						try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
							User user = parentGroup.createUser(requestModel.getUsername());
							user.setFirstname(requestModel.getFirstname());
							user.setUsername(requestModel.getUsername());
							user.setLastname(requestModel.getLastname());
							user.setEmailAddress(requestModel.getEmailAddress());
							user.setPasswordHash(springConfiguration.passwordEncoder().encode(requestModel.getPassword()));
							user.addGroup(parentGroup);
							requestUser.addCRUDPermissionOnRole(parentGroup, CREATE_PERM, user);
							userCreated.complete(user);
							tx.success();
						}
						User user = userCreated.result();
						transformAndResponde(rc, user);
					}
				}
			});

		});
	}

}
