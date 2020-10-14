package com.gentics.mesh.plugin.export.handler;

import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import com.gentics.mesh.core.data.MeshAuthUser;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

/**
 * Per default this handler forbids anonymous access. If this handler is instantiated with a list of roles. This list will act as white-list. Only users with
 * the given roles will be allowed to access routes where this handler is used.
 */
public class RoleBasedAccessHandler implements Handler<RoutingContext> {
	private List<String> allowedRoles = Collections.emptyList();

	public RoleBasedAccessHandler(List<String> allowedRoles) {
		this.allowedRoles = allowedRoles;
	}

	@Override
	public void handle(RoutingContext rc) {
		User user = rc.user();
		if (user == null) {
			send401(rc);
			return;
		}
		MeshAuthUser meshUser;
		JsonObject principal = user.principal();
		if (principal == null) {
			send401(rc);
			return;
		}
		meshUser = principal.mapTo(MeshAuthUser.class);
		if (meshUser == null || "anonymous".equals(meshUser.getUsername())) {
			send401(rc);
			return;
		}
		if (!allowedRoles.isEmpty()) {
			if (StreamSupport.stream(meshUser.getRoles().spliterator(), false).noneMatch(role -> allowedRoles.contains(role.getName()))) {
				send401(rc);
				return;
			}
		}
		rc.next();

	}

	private void send401(RoutingContext rc) {
		rc.fail(401);
	}
}
