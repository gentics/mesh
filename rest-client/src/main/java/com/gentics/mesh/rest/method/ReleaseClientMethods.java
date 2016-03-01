package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
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

}
