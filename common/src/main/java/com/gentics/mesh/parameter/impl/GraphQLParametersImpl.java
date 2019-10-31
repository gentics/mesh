package com.gentics.mesh.parameter.impl;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.GraphQLParameters;
import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import java.util.Collections;
import java.util.Map;

public class GraphQLParametersImpl extends AbstractParameters implements GraphQLParameters {
	public GraphQLParametersImpl(ActionContext ac) {
		super(ac);
	}

	public GraphQLParametersImpl() {
	}

	@Override
	public void validate() {
		// TODO validate query parameter value
	}

	@Override
	public String getName() {
		return "GraphQL parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		QueryParameter waitParameter = new QueryParameter();
		waitParameter.setDescription(
				"Specify whether search should wait for the search to be idle before responding. Only effective when a query is used inside of the request.");
		waitParameter.setExample("true");
		waitParameter.setRequired(false);
		waitParameter.setType(ParamType.BOOLEAN);

		return Collections.singletonMap(WAIT_PARAMETER_KEY, waitParameter);
	}
}
