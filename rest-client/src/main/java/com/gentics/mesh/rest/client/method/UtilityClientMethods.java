package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

public interface UtilityClientMethods {

	/**
	 * Resolve links in the given string
	 * @param body request body
	 * @param parameters
	 * @return
	 */
	MeshRequest<String> resolveLinks(String body, ParameterProvider... parameters);
}
