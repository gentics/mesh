package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.lang.LanguageListResponse;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

/**
 * REST client methods for language manipulation.
 */
public interface LanguageClientMethods {

	/**
	 * Load the given language.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<LanguageResponse> findLanguageByUuid(String uuid, ParameterProvider... parameters);

	/**
	 * Find the language using the specified tag.
	 * 
	 * @param tag
	 * @param parameters
	 * @return
	 */
	MeshRequest<LanguageResponse> findLanguageByTag(String tag, ParameterProvider... parameters);

	/**
	 * Load multiple languages.
	 * 
	 * @param parameters
	 * @return
	 */
	MeshRequest<LanguageListResponse> findLanguages(ParameterProvider... parameters);
}
