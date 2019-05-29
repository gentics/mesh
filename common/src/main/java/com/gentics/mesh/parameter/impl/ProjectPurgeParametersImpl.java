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

		// before
		QueryParameter sinceParameter = new QueryParameter();
		sinceParameter.setDescription("Limit the purge operation to versions which are older than the given date.");
		sinceParameter.setExample("2019-04-20T18:00Z");
		sinceParameter.setRequired(false);
		sinceParameter.setType(ParamType.STRING);
		parameters.put(BEFORE_QUERY_PARAM_KEY, sinceParameter);

		return parameters;
	}

}
