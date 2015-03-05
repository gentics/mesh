package com.gentics.cailun.core.verticle;

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
import com.gentics.cailun.core.rest.response.GenericErrorResponse;
import com.gentics.cailun.core.rest.response.GenericNotFoundResponse;
import com.gentics.cailun.core.rest.response.GenericSuccessResponse;
import com.gentics.cailun.core.rest.response.RestUser;
import com.gentics.cailun.util.UUIDUtil;

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

		Route getRoute = route("/:uuidOrName").method(GET).handler(rc -> {
			String uuidOrName = rc.request().params().get("uuidOrName");

			if (StringUtils.isEmpty(uuidOrName)) {
				rc.next();
				return;
			}

			/*
			 * Load user by uuid or username
			 */
			User user = null;
			if (UUIDUtil.isUUID(uuidOrName)) {
				user = userService.findByUUID(uuidOrName);
			} else {
				user = userService.findByUsername(uuidOrName);
			}

			if (user != null) {
				if (!checkPermission(rc, user, PermissionType.READ)) {
					return;
				}

				RestUser restUser = userService.transformToRest(user);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(restUser));
			} else {
				String message = i18n.get(rc, "user_not_found", uuidOrName);
				rc.response().setStatusCode(404);
				rc.response().end(toJson(new GenericNotFoundResponse(message)));
			}
		});

		/*
		 * List all users when no parameter was specified
		 */
		route("/").method(GET).handler(rc -> {
			Session session = rc.session();
			Map<String, RestUser> resultMap = new HashMap<>();
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
		route("/:uuidOrName").method(DELETE).handler(rc -> {
			String uuidOrName = rc.request().params().get("uuidOrName");
			if (StringUtils.isEmpty(uuidOrName)) {
				// TODO i18n entry
				String message = i18n.get(rc, "request_parameter_missing", "name/uuid");
				rc.response().setStatusCode(400);
				rc.response().end(toJson(new GenericErrorResponse(message)));
				return;
			}

			// Try to load the user
			User user = null;
			if (UUIDUtil.isUUID(uuidOrName)) {
				user = userService.findByUUID(uuidOrName);
			} else {
				user = userService.findByUsername(uuidOrName);
			}

			// Delete the user or show 404
			if (user != null) {
				if (!checkPermission(rc, user, PermissionType.DELETE)) {
					return;
				}
				userService.delete(user);
				rc.response().setStatusCode(200);
				// TODO better response
				rc.response().end(toJson(new GenericSuccessResponse("OK")));
				return;
			} else {
				String message = i18n.get(rc, "user_not_found", uuidOrName);
				rc.response().setStatusCode(404);
				rc.response().end(toJson(new GenericNotFoundResponse(message)));
				return;
			}

		});
	}

	private void addUpdateHandler() {
		route("/:uuidOrName")
				.method(PUT)
				.consumes(APPLICATION_JSON)
				.handler(rc -> {
					String uuidOrName = rc.request().params().get("uuidOrName");
					if (StringUtils.isEmpty(uuidOrName)) {
						// TODO i18n entry
						String message = i18n.get(rc, "request_parameter_missing", "name/uuid");
						rc.response().setStatusCode(400);
						rc.response().end(toJson(new GenericErrorResponse(message)));
						return;
					}

					RestUser requestModel = fromJson(rc, RestUser.class);
					if (requestModel == null) {
						// TODO exception would be nice, add i18n
						String message = "Could not parse request json.";
						rc.response().setStatusCode(400);
						rc.response().end(toJson(new GenericErrorResponse(message)));
						return;
					}
					if (requestModel.getGroups().isEmpty()) {
						// TODO i18n
						String message = "No groups were specified. You need to specify at least one group for the user.";
						rc.response().setStatusCode(400);
						rc.response().end(toJson(new GenericErrorResponse(message)));
						return;
					}

					Set<Group> groupsForUser = new HashSet<>();
					for (String groupName : requestModel.getGroups()) {
						Group parentGroup = groupService.findByName(groupName);
						if (parentGroup == null) {
							// TODO i18n
							rc.response().setStatusCode(400);
							rc.response().end(toJson(new GenericErrorResponse("Could not find parent group {" + groupName + "}")));
							return;
						}
						groupsForUser.add(parentGroup);
					}

					// Try to load the user
					User user = null;
					if (UUIDUtil.isUUID(uuidOrName)) {
						user = userService.findByUUID(uuidOrName);
					} else {
						user = userService.findByUsername(uuidOrName);
					}

					// Update the user or show 404
					if (user != null) {
						if (!checkPermission(rc, user, PermissionType.UPDATE)) {
							return;
						}

						if (requestModel.getUsername() != null && user.getUsername() != requestModel.getUsername()) {
							if (userService.findByUsername(requestModel.getUsername()) != null) {
								rc.response().setStatusCode(409);
								// TODO i18n
								rc.response().end(
										toJson(new GenericErrorResponse("A user with the username {" + requestModel.getUsername()
												+ "} already exists. Please choose a different username.")));
								return;
							}
							user.setUsername(requestModel.getUsername());
						}

						// Check groups from which the user should be removed
						Set<Group> groupsToBeRemoved = new HashSet<>();
						for (Group group : user.getGroups()) {
							// Check whether the user should be removed from the group
							if (!groupsForUser.contains(group)) {
								if (!checkPermission(rc, group, PermissionType.UPDATE)) {
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
							if (!checkPermission(rc, group, PermissionType.UPDATE)) {
								return;
							}
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
							// TODO log
							// TODO correct msg?
							// TODO i18n
							rc.response().setStatusCode(409);
							rc.response().end(toJson(new GenericErrorResponse("User can't be saved. Unknown error.")));
							return;
						}
						rc.response().setStatusCode(200);
						// TODO better response
						rc.response().end(toJson(new GenericSuccessResponse("OK")));
						return;
					} else {
						String message = i18n.get(rc, "user_not_found", uuidOrName);
						rc.response().setStatusCode(404);
						rc.response().end(toJson(new GenericNotFoundResponse(message)));
						return;
					}

				});
	}

	private void addCreateHandler() {
		route("/").method(POST).consumes(APPLICATION_JSON).handler(rc -> {

			RestUser requestModel = fromJson(rc, RestUser.class);
			if (requestModel == null) {
				// TODO exception would be nice, add i18n
				String message = "Could not parse request json.";
				rc.response().setStatusCode(400);
				rc.response().end(toJson(new GenericErrorResponse(message)));
				return;
			}
			if (StringUtils.isEmpty(requestModel.getUsername()) || StringUtils.isEmpty(requestModel.getPassword())) {
				rc.response().setStatusCode(400);
				// TODO i18n
				rc.response().end(toJson(new GenericErrorResponse("Either username or password was not specified.")));
				return;
			}

			// TODO extract groups from json?
			Set<Group> groupsForUser = new HashSet<>();
			for (String groupName : requestModel.getGroups()) {
				Group parentGroup = groupService.findByName(groupName);
				if (parentGroup == null) {
					// TODO i18n
					rc.response().end(toJson(new GenericErrorResponse("Could not find parent group {" + groupName + "}")));
					return;
				}

				// TODO such implicit permissions must be documented
				if (!checkPermission(rc, parentGroup, PermissionType.UPDATE)) {
					return;
				}
				groupsForUser.add(parentGroup);
			}

			if (groupsForUser.isEmpty()) {
				// TODO i18n
				String message = "No groups were specified. You need to specify at least one group for the user.";
				rc.response().end(toJson(new GenericErrorResponse(message)));
				return;
			}

			if (userService.findByUsername(requestModel.getUsername()) != null) {
				// TODO i18n
				rc.response().setStatusCode(400);
				rc.response().end(toJson(new GenericErrorResponse("Conflicting username")));
				return;
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
			RestUser restUser = userService.transformToRest(user);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(restUser));

		});
	}
}
