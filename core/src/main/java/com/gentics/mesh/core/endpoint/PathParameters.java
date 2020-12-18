package com.gentics.mesh.core.endpoint;

import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.context.InternalActionContext;

import io.vertx.ext.web.RoutingContext;

/**
 * Helper for easy path parameter access.
 */
public class PathParameters {

	public static String TAG_FAMILY_PARAM = "tagFamilyUuid";

	public static String TAG_PARAM = "tagUuid";

	@Getter
	public static String getTagUuid(RoutingContext rc) {
		return rc.request().getParam(TAG_PARAM);
	}

	@Getter
	public static String getTagFamilyUuid(RoutingContext rc) {
		return rc.request().getParam(TAG_FAMILY_PARAM);
	}

	@Getter
	public static String getTagFamilyUuid(InternalActionContext ac) {
		return ac.getParameter(TAG_FAMILY_PARAM);
	}

	@Getter
	public static String getTagUuid(InternalActionContext ac) {
		return ac.getParameter(TAG_PARAM);
	}

}
