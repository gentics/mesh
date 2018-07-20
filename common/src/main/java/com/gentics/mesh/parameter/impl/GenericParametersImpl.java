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

		QueryParameter fieldsParam = new QueryParameter();
		fieldsParam.setDescription("Limit the output to certain fields. This is useful in order to reduce the response JSON overhead.");
		fieldsParam.setType(ParamType.STRING);
		fieldsParam.setDefaultValue("");
		parameters.put(FIELDS_PARAM_KEY, fieldsParam);

		QueryParameter etagParam = new QueryParameter();
		etagParam.setDescription(
			"Parameter which can be used to disable the etag parameter generation and thus increase performance when etags are not needed.");
		etagParam.setType(ParamType.BOOLEAN);
		etagParam.setDefaultValue("true");
		parameters.put(FIELDS_PARAM_KEY, etagParam);

		return parameters;
	}

}
