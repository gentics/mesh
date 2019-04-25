package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

/**
 * Admin specific job client methods.
 */
public interface JobClientMethods {

	/**
	 * Load multiple jobs.
	 * 
	 * @param parameters
	 * @return
	 */
	MeshRequest<JobListResponse> findJobs(PagingParameters... parameters);

	/**
	 * Load a specific job.
	 * 
	 * @param uuid
	 *            Job uuid.
	 * @return
	 */
	MeshRequest<JobResponse> findJobByUuid(String uuid);

	/**
	 * Delete the specific job. Please note that only failed jobs can be deleted.
	 *
	 * @param uuid
	 * @return
	 */
	MeshRequest<EmptyResponse> deleteJob(String uuid);

	/**
	 * Reset the error state of the job. This will enable the job to be executed once again.
	 *
	 * @param uuid
	 * @return
	 */
	MeshRequest<EmptyResponse> resetJob(String uuid);

	/**
	 * Process the job and reset the error state of the job.
	 *
	 * @param uuid
	 * @return
	 */
	MeshRequest<JobResponse> processJob(String uuid);

	/**
	 * Manually invoke the job processing.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invokeJobProcessing();

}
