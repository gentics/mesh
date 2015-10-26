package com.gentics.mesh.query.impl;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.query.QueryParameterProvider;

/**
 * The role permission parameter can be used to set the role parameter name which will cause mesh to add the rolePerm field to the rest response.
 */
public class RolePermissionParameter implements QueryParameterProvider {

	public static final String ROLE_PERMISSION_QUERY_PARAM_KEY = "role";

	private String roleName;

	/**
	 * Set the role name.
	 * 
	 * @param roleName
	 * @return Fluent API
	 */
	public RolePermissionParameter setRoleName(String roleName) {
		this.roleName = roleName;
		return this;
	}

	/**
	 * Return the rolename.
	 * 
	 * @return
	 */
	public String getRoleName() {
		return roleName;
	}

	@Override
	public String getQueryParameters() {
		StringBuilder query = new StringBuilder();

		if (!StringUtils.isEmpty(roleName)) {
			query.append("role=");
			query.append(roleName);
		}
		return query.toString();
	}
}
