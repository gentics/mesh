package com.gentics.cailun.core.verticle;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.ext.apex.core.Session;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
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

		route("/:uuidOrName").method(GET).handler(rc -> {
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

	}

	private void addCreateHandler() {

	}
}
