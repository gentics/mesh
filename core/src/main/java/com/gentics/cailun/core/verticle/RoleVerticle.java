package com.gentics.cailun.core.verticle;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.core.Session;

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
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.service.RoleService;
import com.gentics.cailun.core.rest.response.GenericErrorResponse;
import com.gentics.cailun.core.rest.response.GenericNotFoundResponse;
import com.gentics.cailun.core.rest.response.GenericSuccessResponse;
import com.gentics.cailun.core.rest.response.RestRole;
import com.gentics.cailun.util.UUIDUtil;

@Component
@Scope("singleton")
@SpringVerticle
public class RoleVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private RoleService roleService;

	public RoleVerticle() {
		super("roles");
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

			// Try to load the role
			Role role = null;
			if (UUIDUtil.isUUID(uuidOrName)) {
				role = roleService.findByUUID(uuidOrName);
			} else {
				role = roleService.findByName(uuidOrName);
			}

			// Delete the role or show 404
			if (role != null) {
				if (!checkPermission(rc, role, PermissionType.DELETE)) {
					return;
				}
				roleService.delete(role);
				rc.response().setStatusCode(200);
				// TODO better response
				rc.response().end(toJson(new GenericSuccessResponse("OK")));
				return;
			} else {
				String message = i18n.get(rc, "role_not_found", uuidOrName);
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
					RestRole requestModel = fromJson(rc, RestRole.class);
					if (requestModel == null) {
						// TODO exception would be nice, add i18n
						String message = "Could not parse request json.";
						rc.response().setStatusCode(400);
						rc.response().end(toJson(new GenericErrorResponse(message)));
						return;
					}

					// Try to load the role
					Role role = null;
					if (UUIDUtil.isUUID(uuidOrName)) {
						role = roleService.findByUUID(uuidOrName);
					} else {
						role = roleService.findByName(uuidOrName);
					}

					// Update the role or show 404
					if (role != null) {
						if (!checkPermission(rc, role, PermissionType.UPDATE)) {
							return;
						}

						if (!StringUtils.isEmpty(requestModel.getName()) && role.getName() != requestModel.getName()) {
							if (roleService.findByName(requestModel.getName()) != null) {
								rc.response().setStatusCode(409);
								// TODO i18n
								rc.response().end(
										toJson(new GenericErrorResponse("A role with the name {" + requestModel.getName()
												+ "} already exists. Please choose a different name.")));
								return;
							}
							role.setName(requestModel.getName());
						}
						try {
							role = roleService.save(role);
						} catch (ConstraintViolationException e) {
							// TODO log
							// TODO correct msg?
							// TODO i18n
							rc.response().setStatusCode(409);
							rc.response().end(toJson(new GenericErrorResponse("Role can't be saved. Unknown error.")));
							return;
						}
						rc.response().setStatusCode(200);
						// TODO better response
						rc.response().end(toJson(new GenericSuccessResponse("OK")));
					} else {
						String message = i18n.get(rc, "role_not_found", uuidOrName);
						rc.response().setStatusCode(404);
						rc.response().end(toJson(new GenericNotFoundResponse(message)));
						return;
					}

				});
	}

	private void addReadHandler() {
		route("/:uuidOrName").method(GET).handler(rc -> {
			String uuidOrName = rc.request().params().get("uuidOrName");
			if (uuidOrName == null) {
				// TODO handle this case
			}
			Role role = null;
			if (UUIDUtil.isUUID(uuidOrName)) {
				role = roleService.findByUUID(uuidOrName);
			} else {
				role = roleService.findByName(uuidOrName);
			}

			if (role != null) {
				RestRole restRole = roleService.transformToRest(role);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(restRole));
			} else {
				// TODO i18n error message?
				String message = "Group not found {" + uuidOrName + "}";
				rc.response().setStatusCode(404);
				rc.response().end(toJson(new GenericNotFoundResponse(message)));
			}
		});

		/*
		 * List all roles when no parameter was specified
		 */
		route("/").method(GET).handler(rc -> {
			Session session = rc.session();
			Map<String, RestRole> resultMap = new HashMap<>();
			List<Role> roles = roleService.findAll();
			for (Role role : roles) {
				boolean hasPerm = getAuthService().hasPermission(session.getPrincipal(), new CaiLunPermission(role, PermissionType.READ));
				if (hasPerm) {
					resultMap.put(role.getName(), roleService.transformToRest(role));
				}
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(resultMap));
			return;
		});
	}

	private void addCreateHandler() {
		route("/").method(POST).consumes(APPLICATION_JSON).handler(rc -> {
			RestRole requestModel = fromJson(rc, RestRole.class);
			if (requestModel == null) {
				// TODO exception would be nice, add i18n
				String message = "Could not parse request json.";
				rc.response().setStatusCode(400);
				rc.response().end(toJson(new GenericErrorResponse(message)));
				return;
			}

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.response().setStatusCode(400);
				// TODO i18n
				rc.response().end(toJson(new GenericErrorResponse("The name for the role was not specified.")));
				return;
			}

			if (roleService.findByName(requestModel.getName()) != null) {
				// TODO i18n
				rc.response().setStatusCode(400);
				rc.response().end(toJson(new GenericErrorResponse("Conflicting name")));
				return;
			}

			Role role = new Role(requestModel.getName());
			role = roleService.save(role);
			role = roleService.reload(role);

			RestRole restRole = roleService.transformToRest(role);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(restRole));

		});
	}
}
