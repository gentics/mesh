package com.gentics.mesh.data.dao.util;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.handler.VersionHandler;

@Singleton
public class CommonDaoHelper {
	@Inject
	public CommonDaoHelper() {
	}

	/**
	 * Return the API path to the element.
	 *
	 * @param ac
	 * @param element
	 *
	 * @return API path or null if the element has no public path
	 */
	public String getRootLevelAPIPath(InternalActionContext ac, HibCoreElement element) {
		return VersionHandler.baseRoute(ac) + "/" + getAPISegment(element.getTypeInfo().getType()) + "/" + element.getUuid();
	}

	private String getAPISegment(ElementType type) {
		switch (type) {
			case JOB:
				return "jobs";
			case USER:
				return "users";
			case GROUP:
				return "groups";
			case ROLE:
				return "roles";
			case SCHEMA:
				return "schemas";
			case MICROSCHEMA:
				return "microschemas";
			case PROJECT:
				return "projects";
			case TAGFAMILY:
				return "tagFamilies";
			case TAG:
				return "tags";
			case BRANCH:
				return "branches";
			case NODE:
				return "nodes";
			default:
				throw new RuntimeException("No API path segment for type " + type.name());
		}
	}
}
