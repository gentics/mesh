package com.gentics.mesh.parameter;

public interface ParameterProvider {

	/**
	 * Return the query parameters which do not include the the first &amp; or ? character.
	 * 
	 * @return Query string
	 */
	String getQueryParameters();
}
