package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
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
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// role
		QueryParameter pageParameter = new QueryParameter();
		pageParameter.setDescription(
				"The role permission parameter can be used to set the role parameter value in form of an UUID which will cause mesh to add the rolePerm field to the rest response.");
		pageParameter.setExample("24cf92691c7641158f92691c76c115ef");
		pageParameter.setRequired(false);
		pageParameter.setType(ParamType.STRING);
		parameters.put(ROLE_PERMISSION_QUERY_PARAM_KEY, pageParameter);

		return parameters;
	}

}
