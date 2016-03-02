package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseListResponse;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.release.ReleaseUpdateRequest;
import com.gentics.mesh.query.QueryParameterProvider;

import io.vertx.core.Future;

/**
 * Interface for Release specific rest API methods
 */
public interface ReleaseClientMethods {
	/**
	 * Create a release for the given project.
	 * 
	 * @param projectName
	 * @param releaseCreateRequest
	 * @param parameters
	 * @return
	 */
	Future<ReleaseResponse> createRelease(String projectName, ReleaseCreateRequest releaseCreateRequest, QueryParameterProvider... parameters);

	/**
	 * Find the release with the given uuid in the project with the given name.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	Future<ReleaseResponse> findReleaseByUuid(String projectName, String uuid, QueryParameterProvider... parameters);

	/**
	 * Find all releases within the project with the given name. The query parameters can be used to set paging.
	 * 
	 * @param projectName
	 * @param parameters
	 * @return
	 */
	Future<ReleaseListResponse> findReleases(String projectName, QueryParameterProvider... parameters);

	/**
	 * Update the release.
	 * 
	 * @param uuid
	 * @param request
	 * @return
	 */
	Future<ReleaseResponse> updateRelease(String projectName, String uuid, ReleaseUpdateRequest request);

}
