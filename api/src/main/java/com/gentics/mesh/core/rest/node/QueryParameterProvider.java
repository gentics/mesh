package com.gentics.mesh.core.rest.node;

public interface QueryParameterProvider {

	/**
	 * Return the query parameters which do not include the the first & or ? character.
	 * 
	 * @return
	 */
	String getQueryParameters();
}
