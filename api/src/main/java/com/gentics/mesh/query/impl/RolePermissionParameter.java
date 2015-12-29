package com.gentics.mesh.query.impl;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.query.QueryParameterProvider;

/**
 * The role permission parameter can be used to set the role parameter value in form of an UUID which will cause mesh to add the rolePerm field to the rest
 * response.
 */
public class RolePermissionParameter implements QueryParameterProvider {

	public static final String ROLE_PERMISSION_QUERY_PARAM_KEY = "role";

	private String roleUuid;

	/**
	 * Set the role UUID.
	 * 
	 * @param roleUuid
	 * @return Fluent API
	 */
	public RolePermissionParameter setRoleUuid(String roleUuid) {
		this.roleUuid = roleUuid;
		return this;
	}

	/**
	 * Return the role UUID.
	 * 
	 * @return
	 */
	public String getRoleUuid() {
		return roleUuid;
	}

	@Override
	public String getQueryParameters() {
		StringBuilder query = new StringBuilder();

		if (!StringUtils.isEmpty(roleUuid)) {
			query.append("role=");
			query.append(roleUuid);
		}
		return query.toString();
	}

	@Override
	public String toString() {
		return getQueryParameters();
	}
}
