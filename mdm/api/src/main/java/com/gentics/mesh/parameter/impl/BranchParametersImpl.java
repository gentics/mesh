package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.BranchParameters;

public class BranchParametersImpl extends AbstractParameters implements BranchParameters {

	public BranchParametersImpl(ActionContext ac) {
		super(ac);
	}

	public BranchParametersImpl() {
	}

	@Override
	public void validate() {
		// TODO validate branch parameter value
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// branch
		QueryParameter pageParameter = new QueryParameter();
		pageParameter.setDescription(
				"Specifies the branch to be used for loading data. The latest project branch will be used if this parameter is omitted.");
		pageParameter.setExample("24cf92691c7641158f92691c76c115ef");
		pageParameter.setRequired(false);
		pageParameter.setType(ParamType.STRING);
		parameters.put(BRANCH_QUERY_PARAM_KEY, pageParameter);

		return parameters;
	}

	@Override
	public String getName() {
		return "Branch parameters";
	}

}
