package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.job.HibJob;

/**
 * DAO to access {@link HibJob}.
 */
public interface JobDaoWrapper extends JobDao, OrientDBDaoGlobal<HibJob> {

}
