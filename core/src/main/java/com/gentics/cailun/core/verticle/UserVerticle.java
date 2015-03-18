package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.core.Route;

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
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.user.request.UserCreateRequest;
import com.gentics.cailun.core.rest.user.request.UserUpdateRequest;
import com.gentics.cailun.core.rest.user.response.UserResponse;
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.error.HttpStatusCodeErrorException;

@Component
@Scope("singleton")
@SpringVerticle
public class UserVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private UserService userService;

	@Autowired
	private GroupService groupService;

	public UserVerticle() {
		super("users");
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
	}

	private void addReadHandler() {

		route("/:uuid").method(GET).handler(rc -> {
			User user = getObject(rc, "uuid", PermissionType.READ);
			UserResponse restUser = userService.transformToRest(user);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(restUser));
			return;
		});

		/*
		 * List all users when no parameter was specified
		 */
		route("/").method(GET).handler(rc -> {
			Map<String, UserResponse> resultMap = new HashMap<>();
			// TODO paging
				List<User> users = userService.findAll();
				for (User user : users) {
					boolean hasPerm = hasPermission(rc, user, PermissionType.READ);
					if (hasPerm) {
						resultMap.put(user.getUsername(), userService.transformToRest(user));
					}
				}
				rc.response().setStatusCode(200);
				rc.response().end(toJson(resultMap));
				return;
			});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			User user = getObject(rc, "uuid", PermissionType.DELETE);
			userService.delete(user);
			rc.response().setStatusCode(200);
			String uuid = rc.request().params().get("uuid");
			rc.response().end(toJson(new GenericMessageResponse(i18n.get(rc, "user_deleted", uuid))));
		});
	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON);
		route.handler(rc -> {
			User user = getObject(rc, "uuid", PermissionType.UPDATE);

			String uuid = rc.request().params().get("uuid");

			UserUpdateRequest requestModel = fromJson(rc, UserUpdateRequest.class);
			if (requestModel == null) {
				// TODO add i18n
				String message = "Could not parse request json.";
				throw new HttpStatusCodeErrorException(400, message);
			}
			// We don't handle groups within update requests
			// if (requestModel.getGroups().isEmpty()) {
			// // TODO i18n
			// String message = "No groups were specified. You need to specify at least one group for the user.";
			// rc.response().setStatusCode(400);
			// rc.response().end(toJson(new GenericMessageResponse(message)));
			// return;
			// }

			// Set<Group> groupsForUser = new HashSet<>();
			// for (String groupName : requestModel.getGroups()) {
			// Group parentGroup = groupService.findByName(groupName);
			// if (parentGroup == null) {
			// // TODO i18n
			// String message = "Could not find parent group {" + groupName + "}";
			// throw new HttpStatusCodeErrorException(400, message);
			// }
			// groupsForUser.add(parentGroup);
			// }

			// Update the user or show 404
			if (user != null) {
				failOnMissingPermission(rc, user, PermissionType.UPDATE);

				if (requestModel.getUsername() != null && user.getUsername() != requestModel.getUsername()) {
					if (userService.findByUsername(requestModel.getUsername()) != null) {
						// TODO i18n
						String message = "A user with the username {" + requestModel.getUsername()
								+ "} already exists. Please choose a different username.";
						throw new HttpStatusCodeErrorException(409, message);
					}
					user.setUsername(requestModel.getUsername());
				}

				// // Check groups from which the user should be removed
				// Set<Group> groupsToBeRemoved = new HashSet<>();
				// for (Group group : user.getGroups()) {
				// // Check whether the user should be removed from the group
				// if (!groupsForUser.contains(group)) {
				// if (!hasPermission(rc, group, PermissionType.UPDATE)) {
				// return;
				// } else {
				// groupsToBeRemoved.add(group);
				// }
				// } else {
				// groupsForUser.remove(group);
				// }
				// }
				// for (Group group : groupsToBeRemoved) {
				// user.getGroups().remove(group);
				// }
				//
				// // Add users to the remaining set of groups
				// for (Group group : groupsForUser) {
				// failOnMissingPermission(rc, group, PermissionType.UPDATE);
				// user.getGroups().add(group);
				// }

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
					user.setPasswordHash(springConfig.passwordEncoder().encode(requestModel.getPassword()));
				}

				try {
					user = userService.save(user);
				} catch (ConstraintViolationException e) {
					// TODO correct msg, i18n
					throw new HttpStatusCodeErrorException(409, "User can't be saved. Unknown error.", e);
				}
				rc.response().setStatusCode(200);
				// TODO better response
				rc.response().end(toJson(new GenericMessageResponse("OK")));
				return;
			} else {
				String message = i18n.get(rc, "user_not_found", uuid);
				throw new EntityNotFoundException(message);
			}

		});
	}

	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON);
		route.handler(rc -> {

			UserCreateRequest requestModel = fromJson(rc, UserCreateRequest.class);
			if (requestModel == null) {
				// TODO exception would be nice, add i18n
				String message = "Could not parse request json.";
				rc.response().setStatusCode(400);
				rc.response().end(toJson(new GenericMessageResponse(message)));
				return;
			}
			if (StringUtils.isEmpty(requestModel.getUsername()) || StringUtils.isEmpty(requestModel.getPassword())) {
				// TODO i18n
				String message = "Either username or password was not specified.";
				throw new HttpStatusCodeErrorException(400, message);
			}
			String groupUuid = requestModel.getGroupUuid();
			if (StringUtils.isEmpty(groupUuid)) {
				// TODO i18n
				throw new HttpStatusCodeErrorException(400, i18n.get(rc,
						"No parent group for the user was specified. Please set a parent group uuid."));
			}
			Group group = getObjectByUUID(rc, groupUuid, PermissionType.UPDATE);

			// // TODO extract groups from json?
			// Set<Group> groupsForUser = new HashSet<>();
			// for (String groupName : requestModel.getGroups()) {
			// Group parentGroup = groupService.findByName(groupName);
			// if (parentGroup == null) {
			// // TODO i18n
			// String message = "Could not find parent group {" + groupName + "}";
			// throw new HttpStatusCodeErrorException(400, message);
			// }
			//
			// // TODO such implicit permissions must be documented
			// failOnMissingPermission(rc, parentGroup, PermissionType.UPDATE);
			// groupsForUser.add(parentGroup);
			// }
			//
			// if (groupsForUser.isEmpty()) {
			// // TODO i18n
			// String message = "No groups were specified. You need to specify at least one group for the user.";
			// throw new HttpStatusCodeErrorException(400, message);
			// }
			// Check for conflicting usernames
			if (userService.findByUsername(requestModel.getUsername()) != null) {
				String message = i18n.get(rc, "user_conflicting_username");
				throw new HttpStatusCodeErrorException(409, message);
			}

			User user = new User(requestModel.getUsername());
			user.setFirstname(requestModel.getFirstname());
			user.setLastname(requestModel.getLastname());
			user.setEmailAddress(requestModel.getEmailAddress());
			user.setPasswordHash(springConfig.passwordEncoder().encode(requestModel.getPassword()));
			user = userService.save(user);

			// Add the user to the parent group and reload user
			group.addUser(user);
			group = groupService.save(group);
			// Update uuid - TODO remove once save is transactional
			user = userService.reload(user);

			// for (Group group : groupsForUser) {
			// group.addUser(user);
			// groupService.save(group);
			// }
			user = userService.reload(user);
			// TODO add creator info, add update info to group,
			rc.response().setStatusCode(200);
			rc.response().end(toJson(userService.transformToRest(user)));

		});
	}
}
