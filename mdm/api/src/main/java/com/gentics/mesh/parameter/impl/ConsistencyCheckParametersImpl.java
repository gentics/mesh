package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.ConsistencyCheckParameters;

/**
 * Parameter implementation of {@link ConsistencyCheckParameters}
 */
public class ConsistencyCheckParametersImpl extends AbstractParameters implements ConsistencyCheckParameters {
	/**
	 * Create instance on given action context
	 * @param ac action context
	 */
	public ConsistencyCheckParametersImpl(ActionContext ac) {
		super(ac);
	}

	/**
	 * Create empty instance
	 */
	public ConsistencyCheckParametersImpl() {
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// async
		QueryParameter asyncParameter = new QueryParameter();
		asyncParameter.setDefaultValue("false");
		asyncParameter.setDescription("Specifiy whether the consistency check should be executed asynchronously (as job).");
		asyncParameter.setExample("true");
		asyncParameter.setRequired(false);
		asyncParameter.setType(ParamType.BOOLEAN);
		parameters.put(ASYNC_PARAMETER_KEY, asyncParameter);

		return parameters;
	}

	@Override
	public String getName() {
		return "Consistency check parameters";
	}
}
