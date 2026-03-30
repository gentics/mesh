package com.gentics.mesh.parameter.impl;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.ParamType;
import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.parameter.AbstractParameters;
import com.gentics.mesh.parameter.UpdateParameters;

import io.vertx.core.MultiMap;

/**
 * A {@link UpdateParametersImpl} can be used to add additional update parameters to the rest requests.
 */
public class UpdateParametersImpl extends AbstractParameters implements UpdateParameters {

	public UpdateParametersImpl() {
		super();
	}

	public UpdateParametersImpl(ActionContext ac) {
		super(ac);
	}

	public UpdateParametersImpl(MultiMap parameters) {
		super(parameters);
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		Map<String, QueryParameter> parameters = new HashMap<>();

		// upsert
		QueryParameter perPageParameter = new QueryParameter();
		perPageParameter.setDescription("Whether to create the new entity with the given UUID and parameters, if an existing one was not found.");
		perPageParameter.setExample("true");
		perPageParameter.setRequired(false);
		perPageParameter.setType(ParamType.BOOLEAN);
		parameters.put(UPSERT_PARAMETER_KEY, perPageParameter);

		return parameters;
	}

	@Override
	public String getName() {
		return "Update parameters";
	}

}
