package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.BinaryCheckParameters;

public class BinaryCheckParametersImpl extends AbstractParameters implements BinaryCheckParameters {

	public BinaryCheckParametersImpl(ActionContext ac) {
		super(ac);
	}

	public BinaryCheckParametersImpl() {
		super();
	}

	@Override
	public void validate() {

	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		QueryParameter langParameter = new QueryParameter();
		langParameter.setDescription(".");
		langParameter.setExample("lhdgfsgfvaoyegfy");
		langParameter.setRequired(true);
		langParameter.setType(ParamType.STRING);
		parameters.put(SECRET_PARAMETER_KEY, langParameter);

		return parameters;
	}

	@Override
	public String getName() {
		return "Binary check parameters";
	}
}
