package com.gentics.cailun.core.verticle;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.ext.apex.core.Session;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.core.rest.response.GenericNotFoundResponse;
import com.gentics.cailun.core.rest.response.GenericPermissionDeniedResponse;
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
			if (uuidOrName == null) {
				// TODO handle this case
			}
			User user = null;
			if (UUIDUtil.isUUID(uuidOrName)) {
				user = userService.findByUUID(uuidOrName);
			} else {
				user = userService.findByUsername(uuidOrName);
			}

			if (user != null) {
				RestUser restUser = userService.getResponseObject(user);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(restUser));
			} else {
				String message = i18n.get(rc, "user_not_found", uuidOrName);
				rc.response().setStatusCode(404);
				rc.response().end(toJson(new GenericNotFoundResponse(message)));
			}
		});
	}

	private void addDeleteHandler() {
		route("/:uuidOrName").method(DELETE).handler(rc -> {
			String uuidOrName = rc.request().params().get("uuidOrName");
			User user = null;
			if (UUIDUtil.isUUID(uuidOrName)) {
				user = userService.findByUUID(uuidOrName);
			} else {
				user = userService.findByUsername(uuidOrName);
			}

			if (user != null) {

				Session session = rc.session();
				boolean perm = getAuthService().hasPermission(session.getPrincipal(), new CaiLunPermission(user, PermissionType.DELETE));
				if (!perm) {
					rc.response().setStatusCode(403);
					// TODO i18n
				rc.response().end(toJson(new GenericPermissionDeniedResponse("Missing permission on user {" + user.getUuid() + "}")));
				return;
			}
			userService.delete(user);
			rc.response().setStatusCode(200);
			// TODO better response
			rc.response().end(toJson(new GenericSuccessResponse("OK")));
			return;
		} else {
			String message = i18n.get(rc, "group_not_found", uuidOrName);
			rc.response().setStatusCode(404);
			rc.response().end(toJson(new GenericNotFoundResponse(message)));
			return;
		}

	}	);
	}

	private void addUpdateHandler() {

	}

	private void addCreateHandler() {

	}
}
