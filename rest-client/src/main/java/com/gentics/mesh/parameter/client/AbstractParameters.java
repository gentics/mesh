package com.gentics.mesh.parameter.client;

import java.util.Map;

import org.raml.model.parameter.QueryParameter;

import com.gentics.mesh.parameter.ParameterProvider;

import io.vertx.core.MultiMap;

public abstract class AbstractParameters implements ParameterProvider {

	protected MultiMap parameters;

	@Override
	public String getParameter(String name) {
		return parameters.get(name);
	}

	@Override
	public MultiMap getParameters() {
		return parameters;
	}

	@Override
	public void setParameter(String name, String value) {
		parameters.set(name, value);
	}

	@Override
	public Map<? extends String, ? extends QueryParameter> getRAMLParameters() {
		return null;
	}

}
