package com.gentics.cailun.core.verticle;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.service.RoleService;
import com.gentics.cailun.core.rest.response.GenericNotFoundResponse;
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

		});
	}

	private void addUpdateHandler() {
		route("/:uuidOrName").method(PUT).handler(rc -> {

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
				RestRole restRole = roleService.getReponseObject(role);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(restRole));
			} else {
				// TODO i18n error message?
				String message = "Group not found {" + uuidOrName + "}";
				rc.response().setStatusCode(404);
				rc.response().end(toJson(new GenericNotFoundResponse(message)));
			}
		});
	}

	private void addCreateHandler() {
		route("/:uuidOrName").method(POST).handler(rc -> {

		});
	}
}
