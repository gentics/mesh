package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.FieldsParameters;

public class FieldsParametersImpl extends AbstractParameters implements FieldsParameters {

	public FieldsParametersImpl(ActionContext ac) {
		super(ac);
	}

	public FieldsParametersImpl() {
		super();
	}

	@Override
	public void validate() {

	}

	@Override
	public String getName() {
		return "Fields REST query parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		QueryParameter fieldsParam = new QueryParameter();
		fieldsParam.setDescription("Limit the output to certain fields. This is useful in order to reduce the response JSON overhead.");
		fieldsParam.setType(ParamType.STRING);
		fieldsParam.setDefaultValue("");
		parameters.put(FIELDS_PARAM_KEY, fieldsParam);

		return parameters;
	}
}
