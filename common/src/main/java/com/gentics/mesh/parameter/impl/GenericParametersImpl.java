package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.GenericParameters;

public class GenericParametersImpl extends AbstractParameters implements GenericParameters {

	public GenericParametersImpl(ActionContext ac) {
		super(ac);
	}

	public GenericParametersImpl() {
		super();
	}

	@Override
	public void validate() {

	}

	@Override
	public String getName() {
		return "Generic REST query parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		QueryParameter updateAssigned = new QueryParameter();
		updateAssigned.setDescription("Omit the response from the request. This is useful to speed up element creation.");
		updateAssigned.setType(ParamType.BOOLEAN);
		updateAssigned.setDefaultValue("false");
		parameters.put(OMIT_RESPONSE_PARAM_KEY, updateAssigned);

		return parameters;
	}

}
