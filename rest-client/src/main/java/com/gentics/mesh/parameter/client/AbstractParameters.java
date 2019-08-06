package com.gentics.mesh.parameter.client;

import java.util.HashMap;
import java.util.Map;

import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.parameter.ParameterProvider;


public abstract class AbstractParameters implements ParameterProvider {

	protected Map<String, String> parameters = new HashMap<>();

	@Override
	public String getParameter(String name) {
		return parameters.get(name);
	}

	@Override
	public Map<String, String> getParameters() {
		return parameters;
	}

	@Override
	public void setParameter(String name, String value) {
		parameters.put(name, value);
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		return null;
	}

}
