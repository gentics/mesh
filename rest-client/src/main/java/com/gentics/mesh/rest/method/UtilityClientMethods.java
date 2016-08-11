package com.gentics.mesh.rest.method;

import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.MeshRequest;

public interface UtilityClientMethods {

	/**
	 * Resolve links in the given string
	 * @param body request body
	 * @param parameters
	 * @return
	 */
	MeshRequest<String> resolveLinks(String body, ParameterProvider... parameters);
}
