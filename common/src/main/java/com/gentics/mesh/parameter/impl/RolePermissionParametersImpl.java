package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.RolePermissionParameters;

/**
 * @see RolePermissionParameters
 */
public class RolePermissionParametersImpl extends AbstractParameters implements RolePermissionParameters {

	public RolePermissionParametersImpl(ActionContext ac) {
		super(ac);
	}

	public RolePermissionParametersImpl() {
	}

	@Override
	public String getName() {
		return "Role permission parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// role
		QueryParameter pageParameter = new QueryParameter();
		pageParameter.setDescription(
				"The _role_ query parameter take a UUID of a role and may be used to add permission information to the response via the _rolePerm_ property which lists the permissions for the specified role on the element.\n"
						+ "This may be useful when you are logged in as admin but you want to retrieve the editor role permissions on a given node.\n"
						+ "Endpoint: *" + VersionHandler.CURRENT_API_BASE_PATH + "/:projectName/nodes?role=:roleUuid*");
		pageParameter.setExample("24cf92691c7641158f92691c76c115ef");
		pageParameter.setRequired(false);
		pageParameter.setType(ParamType.STRING);
		parameters.put(ROLE_PERMISSION_QUERY_PARAM_KEY, pageParameter);

		return parameters;
	}

}
