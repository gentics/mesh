package com.gentics.mesh.core.data.job;

import static com.gentics.mesh.core.rest.MeshEvent.JOB_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.JOB_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.JOB_UPDATED;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.rest.job.JobResponse;

/**
 * A job can be added to the {@link JobRoot} vertex. Jobs are used to persist information about long running tasks.
 */
public interface Job extends MeshCoreVertex<JobResponse>, CreatorTrackingVertex, HibJob {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.JOB, JOB_CREATED, JOB_UPDATED, JOB_DELETED);

	String TYPE_PROPERTY_KEY = "type";

	String ERROR_DETAIL_PROPERTY_KEY = "error_detail";

	String ERROR_MSG_PROPERTY_KEY = "error_msg";

	String START_TIMESTAMP_PROPERTY_KEY = "startDate";

	String STOP_TIMESTAMP_PROPERTY_KEY = "stopDate";

	String COMPLETION_COUNT_PROPERTY_KEY = "completionCount";

	String STATUS_PROPERTY_KEY = "status";

	String NODE_NAME_PROPERTY_KEY = "nodeName";

	String WARNING_PROPERTY_KEY = "warnings";

	/**
	 * Set the current node name.
	 */
	default void setNodeName() {
		String nodeName = options().getNodeName();
		setNodeName(nodeName);
	}
}
