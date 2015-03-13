package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
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
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.RoleService;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.role.response.RoleResponse;
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.error.HttpStatusCodeErrorException;

@Component
@Scope("singleton")
@SpringVerticle
public class RoleVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private RoleService roleService;

	@Autowired
	private GroupService groupService;

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

		addRoleGroupHandlers();
	}

	private void addRoleGroupHandlers() {
		route("/:roleUuid/groups/:groupUuid").method(PUT).handler(rc -> {
			String roleUuid = rc.request().params().get("roleUuid");
			String groupUuid = rc.request().params().get("groupUuid");
			Role role = roleService.findByUUID(roleUuid);
			Group group = groupService.findByUUID(groupUuid);
		});
		route("/:roleUuid/groups/:groupUuid").method(DELETE).handler(rc -> {
			String roleUuid = rc.request().params().get("roleUuid");
			String groupUuid = rc.request().params().get("groupUuid");
			Role role = roleService.findByUUID(roleUuid);
			Group group = groupService.findByUUID(groupUuid);
		});

	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				// TODO i18n entry
				String message = i18n.get(rc, "request_parameter_missing", "name/uuid");
				rc.response().setStatusCode(400);
				rc.response().end(toJson(new GenericMessageResponse(message)));
				return;
			}

			// Try to load the role
			Role role = roleService.findByUUID(uuid);

			// Delete the role or show 404
			if (role != null) {
				failOnMissingPermission(rc, role, PermissionType.DELETE);
				roleService.delete(role);
				rc.response().setStatusCode(200);
				// TODO better response
				rc.response().end(toJson(new GenericMessageResponse("OK")));
				return;
			} else {
				String message = i18n.get(rc, "role_not_found", uuid);
				throw new EntityNotFoundException(message);
			}
		});
	}

	private void addUpdateHandler() {
		route("/:uuid")
				.method(PUT)
				.consumes(APPLICATION_JSON)
				.handler(rc -> {
					String uuid = rc.request().params().get("uuid");
					if (StringUtils.isEmpty(uuid)) {
						// TODO i18n entry
						String message = i18n.get(rc, "request_parameter_missing", "uuid");
						rc.response().setStatusCode(400);
						rc.response().end(toJson(new GenericMessageResponse(message)));
						return;
					}
					RoleResponse requestModel = fromJson(rc, RoleResponse.class);
					if (requestModel == null) {
						// TODO exception would be nice, add i18n
						String message = "Could not parse request json.";
						rc.response().setStatusCode(400);
						rc.response().end(toJson(new GenericMessageResponse(message)));
						return;
					}

					// Try to load the role
					Role role = roleService.findByUUID(uuid);

					// Update the role or show 404
					if (role != null) {
						failOnMissingPermission(rc, role, PermissionType.UPDATE);

						if (!StringUtils.isEmpty(requestModel.getName()) && role.getName() != requestModel.getName()) {
							if (roleService.findByName(requestModel.getName()) != null) {
								rc.response().setStatusCode(409);
								// TODO i18n
								rc.response().end(
										toJson(new GenericMessageResponse("A role with the name {" + requestModel.getName()
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
							throw new HttpStatusCodeErrorException(409, "Role can't be saved. Unknown error.",e);
						}
						rc.response().setStatusCode(200);
						// TODO better response
						rc.response().end(toJson(new GenericMessageResponse("OK")));
					} else {
						String message = i18n.get(rc, "role_not_found", uuid);
						throw new EntityNotFoundException(message);
					}

				});
	}

	private void addReadHandler() {
		route("/:uuid").method(GET).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (uuid == null) {
				// TODO handle this case
			}
			Role role = roleService.findByUUID(uuid);

			if (role != null) {
				RoleResponse restRole = roleService.transformToRest(role);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(restRole));
			} else {
				// TODO i18n error message?
				String message = "Group not found {" + uuid + "}";
				rc.response().setStatusCode(404);
				rc.response().end(toJson(new GenericMessageResponse(message)));
			}
		});

		/*
		 * List all roles when no parameter was specified
		 */
		route("/").method(GET).handler(rc -> {
			Session session = rc.session();
			Map<String, RoleResponse> resultMap = new HashMap<>();
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
			RoleResponse requestModel = fromJson(rc, RoleResponse.class);
			if (requestModel == null) {
				// TODO exception would be nice, add i18n
				String message = "Could not parse request json.";
				rc.response().setStatusCode(400);
				rc.response().end(toJson(new GenericMessageResponse(message)));
				return;
			}

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.response().setStatusCode(400);
				// TODO i18n
				rc.response().end(toJson(new GenericMessageResponse("The name for the role was not specified.")));
				return;
			}

			if (roleService.findByName(requestModel.getName()) != null) {
				// TODO i18n
				rc.response().setStatusCode(400);
				rc.response().end(toJson(new GenericMessageResponse("Conflicting name")));
				return;
			}

			Role role = new Role(requestModel.getName());
			role = roleService.save(role);
			role = roleService.reload(role);

			RoleResponse restRole = roleService.transformToRest(role);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(restRole));

		});
	}
}
