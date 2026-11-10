package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import com.gentics.vertx.openapi.model.ParamType;
import com.gentics.vertx.openapi.model.parameters.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.ProjectLoadParameters;

public class ProjectLoadParametersImpl extends AbstractParameters implements ProjectLoadParameters {

	public ProjectLoadParametersImpl(ActionContext ac) {
		super(ac);
	}

	public ProjectLoadParametersImpl() {
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getParameterDefinitions() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		QueryParameter langsParameter = new QueryParameter();
		langsParameter.setDescription("Load the project's assigned languages.");
		langsParameter.setExample("true");
		langsParameter.setRequired(false);
		langsParameter.setType(ParamType.BOOLEAN);
		parameters.put(LANGS_QUERY_PARAM_KEY, langsParameter);

		return parameters;
	}

	@Override
	public String getName() {
		return "Project loading request parameters";
	}
}
