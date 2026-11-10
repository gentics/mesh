package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import com.gentics.vertx.openapi.model.ParamType;
import com.gentics.vertx.openapi.model.parameters.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.UserParameters;

/**
 * @see UserParameters
 */
public class UserParametersImpl extends AbstractParameters implements UserParameters {

	public UserParametersImpl() {
	}

	public UserParametersImpl(String token) {
		super();
		setToken(token);
	}

	public UserParametersImpl(ActionContext ac) {
		super(ac);
	}

	@Override
	public String getName() {
		return "User parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getParameterDefinitions() {
		Map<String, QueryParameter> parameters = new HashMap<>();
		// token
		QueryParameter pageParameter = new QueryParameter();
		pageParameter.setDescription(
			"Token code which can be used to update the user even if the connection is not authenticated. This can be used to implement a password recovery feature.");
		pageParameter.setExample("FDrbBDWRY3aS");
		pageParameter.setRequired(false);
		pageParameter.setType(ParamType.STRING);
		parameters.put(TOKEN_PARAMETER_KEY, pageParameter);

		return parameters;
	}

	@Override
	public void validate() {
	}

}
