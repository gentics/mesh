package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.ParameterProvider;

public class PublishParametersImpl extends AbstractParameters {

	public static final String RECURSIVE_PARAMETER_KEY = "recursive";

	public PublishParametersImpl(ActionContext ac) {
		super(ac);
	}

	public PublishParametersImpl() {
	}

	/**
	 * Set the recursive flag which can be used to recursively publish a subtree of nodes.
	 * 
	 * @param flag
	 * @return
	 */
	public ParameterProvider setRecursive(boolean flag) {
		setParameter(RECURSIVE_PARAMETER_KEY, String.valueOf(flag));
		return this;
	}

	/**
	 * Check whether the recursive flag for recursively publishing is enabled.
	 * 
	 * @return
	 */
	public boolean isRecursive() {
		return BooleanUtils.toBooleanDefaultIfNull(Boolean.valueOf(getParameter(RECURSIVE_PARAMETER_KEY)), false);
	}

	@Override
	public void validate() {
		// TODO validate query parameter value
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// recursive
		QueryParameter recursiveParameter = new QueryParameter();
		recursiveParameter.setDefaultValue("false");
		recursiveParameter.setDescription("Specifiy whether the invoked action should be applied recursively.");
		recursiveParameter.setExample("true");
		recursiveParameter.setRequired(false);
		recursiveParameter.setType(ParamType.BOOLEAN);
		parameters.put(RECURSIVE_PARAMETER_KEY, recursiveParameter);

		return parameters;
	}

}
