package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.lang.LanguageListResponse;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
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
	 * Load all installed languages.
	 * 
	 * @param parameters
	 * @return
	 */
	MeshRequest<LanguageListResponse> findLanguages(ParameterProvider... parameters);

	/**
	 * Load the given project language.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<LanguageResponse> findLanguageByUuid(String projectName, String uuid, ParameterProvider... parameters);

	/**
	 * Find the project language using the specified tag.
	 * 
	 * @param tag
	 * @param parameters
	 * @return
	 */
	MeshRequest<LanguageResponse> findLanguageByTag(String projectName, String tag, ParameterProvider... parameters);

	/**
	 * Load project languages.
	 * 
	 * @param parameters
	 * @return
	 */
	MeshRequest<LanguageListResponse> findLanguages(String projectName, ParameterProvider... parameters);

	/**
	 * Load the given project language.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<ProjectResponse> assignLanguageToProjectByUuid(String projectName, String uuid, ParameterProvider... parameters);

	/**
	 * Find the project language using the specified tag.
	 * 
	 * @param tag
	 * @param parameters
	 * @return
	 */
	MeshRequest<ProjectResponse> assignLanguageToProjectByTag(String projectName, String tag, ParameterProvider... parameters);

	/**
	 * Load the given project language.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<ProjectResponse> unassignLanguageFromProjectByUuid(String projectName, String uuid, ParameterProvider... parameters);

	/**
	 * Find the project language using the specified tag.
	 * 
	 * @param tag
	 * @param parameters
	 * @return
	 */
	MeshRequest<ProjectResponse> unassignLanguageFromProjectByTag(String projectName, String tag, ParameterProvider... parameters);
}
