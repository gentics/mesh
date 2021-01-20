package com.gentics.mesh.core.action;

import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.rest.job.JobResponse;

/**
 * DAO actions for jobs.
 * 
 * TODO MDM merge with {@link JobDao}
 */
public interface JobDAOActions extends DAOActions<HibJob, JobResponse> {

}
