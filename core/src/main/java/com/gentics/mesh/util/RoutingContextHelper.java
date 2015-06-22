package com.gentics.mesh.util;

import io.vertx.ext.web.RoutingContext;

import com.gentics.mesh.core.data.model.tinkerpop.MeshShiroUser;
import com.gentics.mesh.error.HttpStatusCodeErrorException;

public final class RoutingContextHelper {

	public static MeshShiroUser getUser(RoutingContext routingContext) {
		if (routingContext.user() instanceof MeshShiroUser) {
			MeshShiroUser user = (MeshShiroUser) routingContext.user();
			return user;
		}
		//TODO i18n
		throw new HttpStatusCodeErrorException(500, "Could not load request user");
	}
}
