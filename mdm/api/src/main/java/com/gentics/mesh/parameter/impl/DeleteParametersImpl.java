package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.DeleteParameters;

public class DeleteParametersImpl extends AbstractParameters implements DeleteParameters {

	public DeleteParametersImpl(ActionContext ac) {
		super(ac);
	}

	public DeleteParametersImpl() {
	}

	@Override
	public void validate() {
		// TODO validate query parameter value
	}

	@Override
	public String getName() {
		return "Delete parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// recursive
		QueryParameter recursiveParameter = new QueryParameter();
		recursiveParameter.setDefaultValue("false");
		recursiveParameter.setDescription("Specifiy whether deletion should also be applied recursively");
		recursiveParameter.setExample("true");
		recursiveParameter.setRequired(false);
		recursiveParameter.setType(ParamType.BOOLEAN);
		parameters.put(RECURSIVE_PARAMETER_KEY, recursiveParameter);

		return parameters;
	}

}
