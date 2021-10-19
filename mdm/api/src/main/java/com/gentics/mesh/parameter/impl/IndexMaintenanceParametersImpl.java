package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.IndexMaintenanceParameters;

/**
 * Parameter implementation for index maintenance parameters
 */
public class IndexMaintenanceParametersImpl extends AbstractParameters implements IndexMaintenanceParameters {
	/**
	 * Create empty instance
	 */
	public IndexMaintenanceParametersImpl() {
	}

	/**
	 * Create instance with parameters filled from the action context
	 * @param ac action context
	 */
	public IndexMaintenanceParametersImpl(ActionContext ac) {
		super(ac);
	}

	@Override
	public String getName() {
		return "Index Maintenance Parameters";
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// index parameter
		QueryParameter indexParameter = new QueryParameter();
		indexParameter.setDescription("Index pattern to handle");
		indexParameter.setExample("node-.*");
		indexParameter.setRequired(false);
		indexParameter.setType(ParamType.STRING);
		parameters.put(INDEX_PARAMETER_KEY, indexParameter);
		return parameters;
	}
}
