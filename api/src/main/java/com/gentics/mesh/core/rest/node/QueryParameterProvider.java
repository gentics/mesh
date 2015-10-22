package com.gentics.mesh.core.rest.node;

public interface QueryParameterProvider {

	/**
	 * Return the query parameters which do not include the the first &amp; or ? character.
	 * 
	 * @return Query string
	 */
	String getQueryParameters();
}
