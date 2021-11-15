package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.JobParameters;

/**
 * Implementation of the job parameters
 */
public class JobParametersImpl extends AbstractParameters implements JobParameters {
	/**
	 * Create empty implementation
	 */
	public JobParametersImpl() {
		super();
	}

	/**
	 * Create implementation which extracts the parameters from the action context
	 * @param ac action context
	 */
	public JobParametersImpl(ActionContext ac) {
		super(ac);
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		QueryParameter statusParam = new QueryParameter();
		statusParam.setDescription("Parameter for filtering jobs by their status. Multiple stati can be given separated by commas.");
		statusParam.setType(ParamType.STRING);
		statusParam.setDefaultValue("");
		parameters.put(STATUS_PARAMETER_KEY, statusParam);

		QueryParameter typeParam = new QueryParameter();
		typeParam.setDescription(
			"Parameter for filtering jobs by their type. Multiple types can be given separated by commas.");
		typeParam.setType(ParamType.STRING);
		typeParam.setDefaultValue("");
		parameters.put(TYPE_PARAMETER_KEY, typeParam);

		return parameters;
	}

	@Override
	public String getName() {
		return "Job List query Parameters";
	}

}
