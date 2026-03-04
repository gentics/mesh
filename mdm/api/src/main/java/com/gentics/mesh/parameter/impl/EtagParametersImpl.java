package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.EtagParameters;

/**
 * @see EtagParameters
 */
public class EtagParametersImpl extends AbstractParameters implements EtagParameters {

	public EtagParametersImpl(ActionContext ac) {
		super(ac);
	}

	public EtagParametersImpl() {
	}

	@Override
	public String getName() {
		return "ETAG REST query parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		QueryParameter etagParam = new QueryParameter();
		etagParam.setDescription(
			"Parameter which can be used to disable the etag parameter generation and thus increase performance when etags are not needed.");
		etagParam.setType(ParamType.BOOLEAN);
		etagParam.setDefaultValue("true");
		parameters.put(ETAG_PARAM_KEY, etagParam);

		return parameters;
	}
}
