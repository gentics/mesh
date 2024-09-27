package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.CacheClearParameters;

public class CacheClearParametersImpl extends AbstractParameters implements CacheClearParameters {
	/**
	 * Create instance on given action context
	 * @param ac action context
	 */
	public CacheClearParametersImpl(ActionContext ac) {
		super(ac);
	}

	/**
	 * Create empty instance
	 */
	public CacheClearParametersImpl() {
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// async
		QueryParameter asyncParameter = new QueryParameter();
		asyncParameter.setDefaultValue("false");
		asyncParameter.setDescription("Specifiy whether the image cache should be cleared as well.");
		asyncParameter.setExample("true");
		asyncParameter.setRequired(false);
		asyncParameter.setType(ParamType.BOOLEAN);
		parameters.put(CLEAR_IMAGE_CACHE_KEY, asyncParameter);

		return parameters;
	}

	@Override
	public String getName() {
		return "Cache clear parameters";
	}
}
