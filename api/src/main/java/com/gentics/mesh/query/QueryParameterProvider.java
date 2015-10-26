package com.gentics.mesh.query;

public interface QueryParameterProvider {

	/**
	 * Return the query parameters which do not include the the first &amp; or ? character.
	 * 
	 * @return Query string
	 */
	String getQueryParameters();
}
