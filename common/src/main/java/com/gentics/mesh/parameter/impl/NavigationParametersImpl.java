package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.NavigationParameters;

public class NavigationParametersImpl extends AbstractParameters implements NavigationParameters {

	public NavigationParametersImpl(ActionContext ac) {
		super(ac);
		// TODO assert values (depth must not be negative etc)
	}

	public NavigationParametersImpl() {
		super();
	}

	@Override
	public void validate() {
		// TODO Auto-generated method stub
	}

	@Override
	public String getName() {
		return "Navigation parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// includeAll
		QueryParameter includeAllParameter = new QueryParameter();
		includeAllParameter.setDescription(
				"If set to true all nodes will be included in the response. By default only container nodes are included in a navigation response.");
		includeAllParameter.setExample("true");
		includeAllParameter.setRequired(false);
		includeAllParameter.setType(ParamType.BOOLEAN);
		parameters.put(INCLUDE_ALL_QUERY_PARAM_KEY, includeAllParameter);

		// maxDepth
		QueryParameter maxDepthParameter = new QueryParameter();
		maxDepthParameter.setDescription("Specifies the maximum depth for the requested navigation tree structure.");
		maxDepthParameter.setExample("5");
		maxDepthParameter.setDefaultValue("10");
		maxDepthParameter.setRequired(false);
		maxDepthParameter.setType(ParamType.NUMBER);
		parameters.put(MAX_DEPTH_QUERY_PARAM_KEY, maxDepthParameter);

		return parameters;
	}

}
