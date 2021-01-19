package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.BackupParameters;

/**
 * @see BackupParameters
 */
public class BackupParametersImpl extends AbstractParameters implements BackupParameters {

	public BackupParametersImpl(ActionContext ac) {
		super(ac);
	}

	public BackupParametersImpl() {
	}

	@Override
	public void validate() {
	}

	@Override
	public String getName() {
		return "Backup parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// consistency check
		QueryParameter consistencyCheck = new QueryParameter();
		consistencyCheck.setDefaultValue("false");
		consistencyCheck.setDescription("Specify whether the consistency check should be run before running the backup.");
		consistencyCheck.setExample("true");
		consistencyCheck.setRequired(false);
		consistencyCheck.setType(ParamType.BOOLEAN);
		parameters.put(CONSISTENCY_CHECK_PARAMETER_KEY, consistencyCheck);

		return parameters;
	}

}
