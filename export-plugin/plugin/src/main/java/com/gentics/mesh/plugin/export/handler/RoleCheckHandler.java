package com.gentics.mesh.plugin.export.handler;

import com.gentics.mesh.plugin.PluginContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RoleCheckHandler implements Handler<PluginContext> {

	private final String roleName;

	public RoleCheckHandler(String roleName) {
		this.roleName = roleName;
	}

	@Override
	public void handle(PluginContext pc) {
		JsonObject data = pc.user().principal();
		JsonArray roles = data.getJsonArray("roles", new JsonArray());

		// Check if it is an admin or if it has the role. If it doesn't, stop the request.
		if (!data.getBoolean("admin", false) && !this.hasRole(roles, this.roleName)) {
			pc.response().setStatusCode(HttpResponseStatus.FORBIDDEN.code()).end(new JsonObject()
				.put("status", "forbidden")
				.put("message", "You don't have the required permission to perform this action!")
				.encodePrettily()
			);
			return;
		}

		pc.next();
	}

	private boolean hasRole(JsonArray roles, String roleName) {
		return roles.stream()
			.filter(role -> role != null && role instanceof JsonObject)
			.anyMatch(role -> roleName.equals(((JsonObject) role).getString("name")));
	}
}
