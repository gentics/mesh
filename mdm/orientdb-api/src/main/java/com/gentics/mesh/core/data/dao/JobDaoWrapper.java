package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.rest.job.JobResponse;

/**
 * DAO to access {@link HibJob}.
 */
public interface JobDaoWrapper extends JobDao, DaoTransformable<HibJob, JobResponse> {


}
