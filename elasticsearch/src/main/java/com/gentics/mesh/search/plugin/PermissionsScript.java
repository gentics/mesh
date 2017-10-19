package com.gentics.mesh.search.plugin;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.script.AbstractSearchScript;

import com.gentics.mesh.search.impl.MeshNode;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Custom script which will filter out documents which would not be visible by the given user role uuids. The script will compare the user roles with the roles
 * that are stored along with the document.
 */
public class PermissionsScript extends AbstractSearchScript {

	private static final Logger log = LoggerFactory.getLogger(PermissionsScript.class);

	private static final String ROLE_UUID_FIELD = "_roleUuids";

	private static final String USER_ROLE_UUIDS_SCRIPT_PARAM = "userRoleUuids";

	private List<?> userRoleUuids;

	public PermissionsScript(Map<String, Object> params, MeshNode node) {
		Object u2 = params.get(USER_ROLE_UUIDS_SCRIPT_PARAM);
		Objects.requireNonNull(u2, "The userRoles parameter must be set for the permission script");
		if (u2 instanceof List) {
			this.userRoleUuids = (List<?>) u2;
		}
	}

	@Override
	public Object run() {
		if (log.isTraceEnabled()) {
			String uuid = (String) source().get("uuid");
			log.trace("Checking permission on element with uuid {" + uuid + "}");
		}
		Object ids = source().get(ROLE_UUID_FIELD);
		if (ids instanceof List) {
			List<?> documentRoleUuids = (List<?>) ids;
			// Check if the user has at least one granting role.
			boolean hasRole = CollectionUtils.containsAny(userRoleUuids, documentRoleUuids);
			System.out.println("Element " + source().get("uuid") + " " + hasRole);
			return hasRole;
		} else {
			log.warn("Document {" + source() + "} did not contain the " + ROLE_UUID_FIELD + " field.");
		}
		return false;
	}

}