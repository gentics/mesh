package com.gentics.mesh.core.data.job;

import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.rest.job.JobResponse;

/**
 * A job can be added to the {@link JobRoot} vertex. Jobs are used to persist information about long running tasks.
 */
public interface Job extends MeshCoreVertex<JobResponse>, CreatorTrackingVertex, HibJob {

}
