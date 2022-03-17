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

		parameters.put(STATUS_PARAMETER_KEY, createQueryParameter(
				"Parameter for filtering jobs by their status. Multiple values can be given separated by commas."));
		parameters.put(TYPE_PARAMETER_KEY, createQueryParameter(
				"Parameter for filtering jobs by their type. Multiple values can be given separated by commas."));
		parameters.put(BRANCH_NAME_PARAMETER_KEY, createQueryParameter(
				"Parameter for filtering jobs by the branch name. Multiple values can be given separated by commas."));
		parameters.put(BRANCH_UUID_PARAMETER_KEY, createQueryParameter(
				"Parameter for filtering jobs by the branch uuid. Multiple values can be given separated by commas."));
		parameters.put(SCHEMA_NAME_PARAMETER_KEY, createQueryParameter(
				"Parameter for filtering jobs by the schema name. Multiple values can be given separated by commas."));
		parameters.put(SCHEMA_UUID_PARAMETER_KEY, createQueryParameter(
				"Parameter for filtering jobs by the schema uuid. Multiple values can be given separated by commas."));
		parameters.put(MICROSCHEMA_NAME_PARAMETER_KEY, createQueryParameter(
				"Parameter for filtering jobs by the microschema name. Multiple values can be given separated by commas."));
		parameters.put(MICROSCHEMA_UUID_PARAMETER_KEY, createQueryParameter(
				"Parameter for filtering jobs by the microschema uuid. Multiple values can be given separated by commas."));
		parameters.put(FROM_VERSION_PARAMETER_KEY, createQueryParameter(
				"Parameter for filtering jobs by the 'from' version. Multiple values can be given separated by commas."));
		parameters.put(TO_VERSION_PARAMETER_KEY, createQueryParameter(
				"Parameter for filtering jobs by the 'to' version. Multiple values can be given separated by commas."));

		return parameters;
	}

	@Override
	public String getName() {
		return "Job List query Parameters";
	}

	/**
	 * Create a query parameter of type {@link ParamType#STRING} and empty default value
	 * @param description parameter description
	 * @return query parameter
	 */
	protected QueryParameter createQueryParameter(String description) {
		QueryParameter param = new QueryParameter();
		param.setDescription(description);
		param.setType(ParamType.STRING);
		param.setDefaultValue("");
		return param;
	}
}