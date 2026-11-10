package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import com.gentics.vertx.openapi.model.ParamType;
import com.gentics.vertx.openapi.model.parameters.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.GenericParameters;

/**
 * @see GenericParameters
 */
public class GenericParametersImpl extends EtagParametersImpl implements GenericParameters {

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
	public Map<? extends String, ? extends QueryParameter> getParameterDefinitions() {
		Map<String, QueryParameter> parameters = new HashMap<>(super.getParameterDefinitions());

		QueryParameter fieldsParam = new QueryParameter();
		fieldsParam.setDescription("Limit the output to certain fields. This is useful in order to reduce the response JSON overhead.");
		fieldsParam.setType(ParamType.STRING);
		fieldsParam.setDefaultValue("");
		fieldsParam.setExample("uuid,name");
		parameters.put(FIELDS_PARAM_KEY, fieldsParam);

		return parameters;
	}

}
