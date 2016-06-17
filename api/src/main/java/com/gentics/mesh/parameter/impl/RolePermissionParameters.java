package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.handler.ActionContext;

/**
 * The role permission parameter can be used to set the role parameter value in form of an UUID which will cause mesh to add the rolePerm field to the rest
 * response.
 */
public class RolePermissionParameters extends AbstractParameters {

	public static final String ROLE_PERMISSION_QUERY_PARAM_KEY = "role";

	private String roleUuid;

	public RolePermissionParameters(ActionContext ac) {
		super(ac);
	}

	public RolePermissionParameters() {
	}

	/**
	 * Set the role UUID.
	 * 
	 * @param roleUuid
	 * @return Fluent API
	 */
	public RolePermissionParameters setRoleUuid(String roleUuid) {
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
	protected Map<String, Object> getParameters() {
		Map<String, Object> map = new HashMap<>();
		map.put(ROLE_PERMISSION_QUERY_PARAM_KEY, roleUuid);
		return map;
	}

	@Override
	protected void constructFrom(ActionContext ac) {
		this.roleUuid = ac.getParameter(ROLE_PERMISSION_QUERY_PARAM_KEY);
	}

}
