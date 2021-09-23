package com.gentics.mesh.core.data.job;

import static com.gentics.mesh.core.rest.MeshEvent.JOB_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.JOB_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.JOB_UPDATED;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.util.DateUtils;

import io.reactivex.Completable;

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
	 * The max length before detail error messages will be truncated
	 */
	int ERROR_DETAIL_MAX_LENGTH = 50000;

	/**
	 * Return the job type.
	 * 
	 * @return
	 */
	JobType getType();

	/**
	 * Set the job type.
	 * 
	 * @param type
	 */
	void setType(JobType type);

	/**
	 * Return the branch reference for the job.
	 * 
	 * @return
	 */
	HibBranch getBranch();

	/**
	 * Set the branch reference for the job.
	 * 
	 * @param branch
	 */
	void setBranch(HibBranch branch);

	/**
	 * Return the schema version reference.
	 * 
	 * @return
	 */
	HibSchemaVersion getFromSchemaVersion();

	/**
	 * Set the schema version reference.
	 * 
	 * @param version
	 */
	void setFromSchemaVersion(HibSchemaVersion version);

	/**
	 * Return the schema version reference.
	 * 
	 * @return
	 */
	HibSchemaVersion getToSchemaVersion();

	/**
	 * Set the schema version reference.
	 * 
	 * @param version
	 */
	void setToSchemaVersion(HibSchemaVersion version);

	/**
	 * Return the microschema version reference.
	 * 
	 * @return
	 */
	HibMicroschemaVersion getFromMicroschemaVersion();

	/**
	 * Set the microschema version reference.
	 * 
	 * @param fromVersion
	 */
	void setFromMicroschemaVersion(HibMicroschemaVersion fromVersion);

	/**
	 * Return the microschema version reference.
	 * 
	 * @return
	 */
	HibMicroschemaVersion getToMicroschemaVersion();

	/**
	 * Set the microschema version reference.
	 * 
	 * @param toVersion
	 */
	void setToMicroschemaVersion(HibMicroschemaVersion toVersion);

	/**
	 * Process the job.
	 */
	Completable process();

	/**
	 * Mark the job as failed.
	 * 
	 * @param e
	 */
	void markAsFailed(Exception e);

	/**
	 * Set the error information using the provided exception.
	 * 
	 * @param e
	 */
	void setError(Throwable e);

	/**
	 * Return the start date of the job.
	 * 
	 * @return
	 */
	default String getStartDate() {
		Long timestamp = getStartTimestamp();
		if (timestamp == null) {
			return null;
		}
		return DateUtils.toISO8601(timestamp);
	}

	/**
	 * Return the start timestamp of the job.
	 * 
	 * @return
	 */
	Long getStartTimestamp();

	/**
	 * Set the start timestamp of the job.
	 * 
	 * @param date
	 */
	void setStartTimestamp(Long date);

	/**
	 * Set the current start timestamp.
	 */
	default void setStartTimestamp() {
		setStartTimestamp(System.currentTimeMillis());
	}

	/**
	 * Return the stop date of the job.
	 * 
	 * @return
	 */
	default String getStopDate() {
		Long timestamp = getStopTimestamp();
		if (timestamp == null) {
			return null;
		}
		return DateUtils.toISO8601(timestamp);
	}

	/**
	 * Return the stop timestamp of the job.
	 * 
	 * @return
	 */
	Long getStopTimestamp();

	/**
	 * Set the stop date of the job.
	 * 
	 * @param date
	 */
	void setStopTimestamp(Long date);

	/**
	 * Set the current stop timestamp.
	 */
	default void setStopTimestamp() {
		setStopTimestamp(System.currentTimeMillis());
	}

	/**
	 * Return the amount of elements which have already been processed.
	 * 
	 * @return
	 */
	long getCompletionCount();

	/**
	 * Set the count of total processed elements.
	 * 
	 * @param count
	 */
	void setCompletionCount(long count);

	/**
	 * Return the name of the node on which the job is being executed.
	 * 
	 * @return
	 */
	String getNodeName();

	/**
	 * Set the name of the node on which the job is being executed.
	 * 
	 * @param nodeName
	 */
	void setNodeName(String nodeName);

	/**
	 * Set the current node name.
	 */
	default void setNodeName() {
		String nodeName = options().getNodeName();
		setNodeName(nodeName);
	}

	/**
	 * Return the stored warnings.
	 * 
	 * @return
	 */
	JobWarningList getWarnings();

	/**
	 * Set the list of warnings.
	 * 
	 * @param warning
	 */
	void setWarnings(JobWarningList warnings);

}
