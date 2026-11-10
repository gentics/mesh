package com.gentics.mesh.parameter.impl;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.SearchParameters;
import com.gentics.vertx.openapi.model.ParamType;
import com.gentics.vertx.openapi.model.parameters.QueryParameter;

import java.util.Collections;
import java.util.Map;

/**
 * @see SearchParameters
 */
public class SearchParametersImpl extends AbstractParameters implements SearchParameters {

	public SearchParametersImpl(ActionContext ac) {
		super(ac);
	}

	public SearchParametersImpl() {
	}

	@Override
	public void validate() {
		// TODO validate query parameter value
	}

	@Override
	public String getName() {
		return "Search parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getParameterDefinitions() {
		QueryParameter waitParameter = new QueryParameter();
		waitParameter.setDescription("Specify whether search should wait for the search to be idle before responding.");
		waitParameter.setExample("true");
		waitParameter.setRequired(false);
		waitParameter.setType(ParamType.BOOLEAN);

		return Collections.singletonMap(WAIT_PARAMETER_KEY, waitParameter);
	}

}
