package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.core.Route;
import io.vertx.ext.apex.core.Session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		addUserGroupHandlers();

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();

	}

	private void addUserGroupHandlers() {
		Route putRoute = route("/:userUuid/groups/:groupUuid").method(PUT);
		putRoute.handler(rc -> {
			String userUuid = rc.request().params().get("userUuid");
			String groupUuid = rc.request().params().get("groupUuid");
			User user = userService.findByUUID(userUuid);
			if (user == null) {
				throw new EntityNotFoundException(i18n.get(rc, "user_not_found", userUuid));
			}
			Group group = groupService.findByUUID(groupUuid);
			if (group == null) {
				throw new EntityNotFoundException(i18n.get(rc, "group_not_found_for_uuid", groupUuid));
			}

			throw new HttpStatusCodeErrorException(501, "Not implemented");
		});

		Route deleteRoute = route("/:userUuid/groups/:groupUuid").method(DELETE);
		deleteRoute.handler(rc -> {
			String userUuid = rc.request().params().get("userUuid");
			String groupUuid = rc.request().params().get("groupUuid");
			User user = userService.findByUUID(userUuid);
			if (user == null) {
				throw new EntityNotFoundException(i18n.get(rc, "user_not_found", userUuid));
			}
			failOnMissingPermission(rc, user, PermissionType.READ);

			Group group = groupService.findByUUID(groupUuid);
			if (group == null) {
				throw new EntityNotFoundException(i18n.get(rc, "group_not_found_for_uuid", groupUuid));
			}
			failOnMissingPermission(rc, group, PermissionType.UPDATE);

			if (!group.hasUser(user)) {
				throw new HttpStatusCodeErrorException(400, "User is not a member of the group.");
			}

			// TODO check whether this would be the last group of the user
				if (userService.removeUserFromGroup(user, group)) {
					rc.response().setStatusCode(200);
					rc.response().end(
							toJson(new GenericMessageResponse("Removed user {" + user.getUsername() + "} from group {" + group.getName() + "}")));
				} else {
					throw new HttpStatusCodeErrorException(501, "Error while removing user from group.");
				}
			});
	}

	private void addReadHandler() {

		route("/:uuid").method(GET).handler(rc -> {
			String uuid = rc.request().params().get("uuid");

			// TODO do we really need this?
				if (StringUtils.isEmpty(uuid)) {
					rc.next();
					return;
				}

				User user = userService.findByUUID(uuid);
				if (user == null) {
					String message = i18n.get(rc, "user_not_found", uuid);
					throw new EntityNotFoundException(message);
				} else {
					failOnMissingPermission(rc, user, PermissionType.READ);
					UserResponse restUser = userService.transformToRest(user);
					rc.response().setStatusCode(200);
					rc.response().end(toJson(restUser));
					return;
				}
			});

		/*
		 * List all users when no parameter was specified
		 */
		route("/").method(GET).handler(rc -> {
			Session session = rc.session();
			Map<String, UserResponse> resultMap = new HashMap<>();
			List<User> users = userService.findAll();
			for (User user : users) {
				boolean hasPerm = getAuthService().hasPermission(session.getPrincipal(), new CaiLunPermission(user, PermissionType.READ));
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
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				// TODO i18n entry
				String message = i18n.get(rc, "request_parameter_missing", "uuid");
				throw new HttpStatusCodeErrorException(400, message);
			}

			// Try to load the user
			User user = userService.findByUUID(uuid);

			// Delete the user or show 404
			if (user != null) {
				failOnMissingPermission(rc, user, PermissionType.DELETE);
				userService.delete(user);
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

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				String message = i18n.get(rc, "request_parameter_missing", "uuid");
				throw new HttpStatusCodeErrorException(400, message);
			}

			UserUpdateRequest requestModel = fromJson(rc, UserUpdateRequest.class);
			if (requestModel == null) {
				// TODO add i18n
				String message = "Could not parse request json.";
				throw new HttpStatusCodeErrorException(400, message);
			}
			if (requestModel.getGroups().isEmpty()) {
				// TODO i18n
				String message = "No groups were specified. You need to specify at least one group for the user.";
				rc.response().setStatusCode(400);
				rc.response().end(toJson(new GenericMessageResponse(message)));
				return;
			}

			Set<Group> groupsForUser = new HashSet<>();
			for (String groupName : requestModel.getGroups()) {
				Group parentGroup = groupService.findByName(groupName);
				if (parentGroup == null) {
					// TODO i18n
					String message = "Could not find parent group {" + groupName + "}";
					throw new HttpStatusCodeErrorException(400, message);
				}
				groupsForUser.add(parentGroup);
			}

			// Try to load the user
			User user = userService.findByUUID(uuid);

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

				// Check groups from which the user should be removed
				Set<Group> groupsToBeRemoved = new HashSet<>();
				for (Group group : user.getGroups()) {
					// Check whether the user should be removed from the group
					if (!groupsForUser.contains(group)) {
						if (!hasPermission(rc, group, PermissionType.UPDATE)) {
							return;
						} else {
							groupsToBeRemoved.add(group);
						}
					} else {
						groupsForUser.remove(group);
					}
				}
				for (Group group : groupsToBeRemoved) {
					user.getGroups().remove(group);
				}

				// Add users to the remaining set of groups
				for (Group group : groupsForUser) {
					failOnMissingPermission(rc, group, PermissionType.UPDATE);
					user.getGroups().add(group);
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

			// TODO extract groups from json?
			Set<Group> groupsForUser = new HashSet<>();
			for (String groupName : requestModel.getGroups()) {
				Group parentGroup = groupService.findByName(groupName);
				if (parentGroup == null) {
					// TODO i18n
					String message = "Could not find parent group {" + groupName + "}";
					throw new HttpStatusCodeErrorException(400, message);
				}

				// TODO such implicit permissions must be documented
				failOnMissingPermission(rc, parentGroup, PermissionType.UPDATE);
				groupsForUser.add(parentGroup);
			}

			if (groupsForUser.isEmpty()) {
				// TODO i18n
				String message = "No groups were specified. You need to specify at least one group for the user.";
				throw new HttpStatusCodeErrorException(400, message);
			}

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
			// Update uuid - TODO remove once save is transactional

			for (Group group : groupsForUser) {
				group.addUser(user);
				groupService.save(group);
			}
			user = userService.reload(user);
			// TODO add creator info, add update info to group,
			UserResponse restUser = userService.transformToRest(user);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(restUser));

		});
	}
}
