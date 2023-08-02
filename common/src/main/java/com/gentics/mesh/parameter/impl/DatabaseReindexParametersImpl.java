package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.DatabaseReindexParameters;

public class DatabaseReindexParametersImpl extends AbstractParameters implements DatabaseReindexParameters {

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		QueryParameter consistencyCheck = new QueryParameter();
		consistencyCheck.setDescription("Specify a comma separated list of database indices, that have to be rebuilt, leaving all other indices untouched.");
		consistencyCheck.setExample("e.has_field_container_field,e.has_field_container_branch_type_lang");
		consistencyCheck.setRequired(false);
		consistencyCheck.setType(ParamType.STRING);
		parameters.put(LIMIT_TO_PARAMETER_KEY, consistencyCheck);

		return parameters;
	}

	@Override
	public String getName() {
		return "Database reindex parameters";
	}
}
