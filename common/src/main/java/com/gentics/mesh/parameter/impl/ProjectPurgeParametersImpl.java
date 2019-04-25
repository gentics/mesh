package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.ProjectPurgeParameters;

public class ProjectPurgeParametersImpl extends AbstractParameters implements ProjectPurgeParameters {

	public ProjectPurgeParametersImpl(ActionContext ac) {
		super(ac);
	}

	public ProjectPurgeParametersImpl() {
	}

	@Override
	public String getName() {
		return "Project version purge request parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// since
		QueryParameter sinceParameter = new QueryParameter();
		// TODO description
		sinceParameter.setDescription("");
		sinceParameter.setExample("2019-04-20T18:00Z");
		sinceParameter.setRequired(false);
		sinceParameter.setType(ParamType.STRING);
		parameters.put(SINCE_QUERY_PARAM_KEY, sinceParameter);

		return parameters;
	}

}
