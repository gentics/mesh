package com.gentics.mesh.rest.method;

import com.gentics.mesh.parameter.ParameterProvider;

import io.vertx.core.Future;

public interface UtilityClientMethods {

	/**
	 * Resolve links in the given string
	 * @param body request body
	 * @param parameters
	 * @return
	 */
	Future<String> resolveLinks(String body, ParameterProvider... parameters);
}
