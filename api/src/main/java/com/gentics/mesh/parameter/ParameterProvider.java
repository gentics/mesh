package com.gentics.mesh.parameter;

import java.util.Map;

import org.raml.model.parameter.QueryParameter;

public interface ParameterProvider {

	/**
	 * Return the query parameters which do not include the the first &amp; or ? character.
	 * 
	 * @return Query string
	 */
	String getQueryParameters();

	/**
	 * * Validate the parameters and throw an exception when an invalid set of parameters has been detected.
	 */
	void validate();

	Map<? extends String, ? extends QueryParameter> getRAMLParameters();
}
